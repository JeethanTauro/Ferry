import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../../context/AuthContext.jsx";

import { getEndpoint, getWebhookEvents, replayWebhookEvent } from "../../services/api.js";

import "./EndpointDashboard.css";

function EndpointDashboard() {

  const { id } = useParams();

  const [endpoint, setEndpoint] = useState(null);
  const [usage, setUsage] = useState(null);

  const [events, setEvents] = useState([]);
  const { user, loading: authLoading } = useAuth();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [replayingEventId, setReplayingEventId] = useState(null);
  const [actionFeedback, setActionFeedback] = useState(null);

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case "DELIVERED":
        return "status-delivered";
      case "FAILED":
        return "status-failed";
      case "DLQ":
        return "status-dlq";
      case "PENDING":
        return "status-pending";
      case "IN_FLIGHT":
        return "status-inflight"
      default:
        return "";
    }
  };

  const handleReplayEvent = async (eventId) => {
    try {
      setReplayingEventId(eventId);
      setActionFeedback(null);

      if (!user) {
        throw new Error("You are not authenticated.");
      }

      const idToken = await user.getIdToken();
      await replayWebhookEvent(idToken, eventId);

      // Optimistically update the event's status to PENDING
      setEvents((prevEvents) =>
        prevEvents.map((ev) =>
          ev.eventId === eventId ? { ...ev, status: "PENDING" } : ev
        )
      );

      setActionFeedback({
        type: "success",
        message: `Event #${eventId} queued for replay successfully.`,
      });
    } catch (err) {
      console.error("Failed to replay event:", err);
      setActionFeedback({
        type: "error",
        message: err.message || "Failed to replay webhook event.",
      });
    } finally {
      setReplayingEventId(null);
    }
  };


  useEffect(() => {

    if (authLoading) {
      return;
    }

    if (!user) {
      setError("You are not authenticated.");
      setLoading(false);
      return;
    }

    const loadEndpoint = async () => {

      try {

        const idToken = await user.getIdToken();

        const data = await getEndpoint(
          idToken,
          id
        );

        const webhookEvents = await getWebhookEvents(idToken,id);
        console.log("Webhook events:", webhookEvents);

        setEndpoint(data.webhookResponse);
        setUsage(data.webhookResponseUsage);
        setEvents(webhookEvents)

      } catch (error) {

        console.error(error);

        setError(
          error.message || "Failed to load endpoint."
        );

      } finally {

        setLoading(false);

      }
    };

    loadEndpoint();

  }, [user, authLoading, id]);


  if (loading) {
    return (
      <main className="endpoint-dashboard">
        <div className="dashboard-state">
          Loading endpoint...
        </div>
      </main>
    );
  }


  if (error) {
    return (
      <main className="endpoint-dashboard">
        <div className="dashboard-state error-state">
          {error}
        </div>
      </main>
    );
  }


  if (!endpoint || !usage) {
    return null;
  }


  return (
    <main className="endpoint-dashboard">

      <section className="dashboard-header">

        <div>

          <Link
            to="/endpoints"
            className="back-link"
          >
            ← Back to Endpoints
          </Link>

          <div className="dashboard-title-row">

            <h1>{endpoint.name}</h1>

            <span
              className={`status-badge ${
                endpoint.active
                  ? "active"
                  : "inactive"
              }`}
            >
              {endpoint.active
                ? "Active"
                : "Disabled"}
            </span>

          </div>

          <p className="dashboard-endpoint-url">
            {endpoint.endpoint}
          </p>

        </div>

      </section>


      <section className="usage-grid">

        <div className="usage-card">
          <span className="usage-label">
            Events Received
          </span>

          <strong>
            {usage.eventsReceived}
          </strong>
        </div>


        <div className="usage-card">
          <span className="usage-label">
            Events Delivered
          </span>

          <strong>
            {usage.eventsDelivered}
          </strong>
        </div>


        <div className="usage-card">
          <span className="usage-label">
            Events Failed
          </span>

          <strong>
            {usage.eventsFailed}
          </strong>
        </div>

      </section>


      <section className="dashboard-details">

        <div className="dashboard-card">

          <h2>Endpoint Details</h2>

          <div className="detail-row">
            <span>Name</span>
            <strong>{endpoint.name}</strong>
          </div>

          <div className="detail-row">
            <span>Endpoint ID</span>
            <strong>{endpoint.endpointId}</strong>
          </div>

          <div className="detail-row">
            <span>Destination</span>
            <strong>{endpoint.destinationUrl}</strong>
          </div>

          <div className="detail-row">
            <span>Rate Limit</span>
            <strong>
              {endpoint.rateLimit} / second
            </strong>
          </div>

          <div className="detail-row">
            <span>Created</span>
            <strong>
              {new Date(
                endpoint.createdAt
              ).toLocaleDateString()}
            </strong>
          </div>

          <div className="detail-row">
            <span>Last Updated</span>
            <strong>
              {new Date(
                endpoint.updatedAt
              ).toLocaleDateString()}
            </strong>
          </div>

        </div>

      </section>

      <section className="dashboard-events">

        <div className="dashboard-card">

          <div className="events-card-header">
            <h2>Webhook Events</h2>
            <span className="events-count-badge">
              {events.length} {events.length === 1 ? "event" : "events"}
            </span>
          </div>

          {actionFeedback && (
            <div className={`dashboard-feedback ${actionFeedback.type}`}>
              <span>{actionFeedback.message}</span>
              <button
                type="button"
                className="feedback-dismiss"
                onClick={() => setActionFeedback(null)}
                aria-label="Dismiss feedback"
              >
                ×
              </button>
            </div>
          )}

          {events.length === 0 ? (

            <p className="empty-events">
              No webhook events received yet.
            </p>

          ) : (

            <div className="events-list">

              {events.map((event, index) => {
                const canReplay = event.status === "FAILED" || event.status === "DLQ";
                const isReplaying = replayingEventId === event.eventId;

                return (
                  <div className="event-card" key={event.eventId || index}>

                    <div className="event-header">

                      <div className="event-header-left">

                        <div className="event-id-chip" title={`Event ID: ${event.eventId}`}>
                          #{event.eventId}
                        </div>

                        <div className="event-header-col">
                          <span className="event-label">
                            Status
                          </span>

                          <span
                            className={`status-badge event-status-badge ${getStatusBadgeClass(
                              event.status
                            )}`}
                          >
                            {event.status}
                          </span>
                        </div>

                        <div className="event-header-col">
                          <span className="event-label">
                            Received At
                          </span>

                          <span className="event-time">
                            {new Date(
                              event.receivedAt
                            ).toLocaleString()}
                          </span>
                        </div>

                      </div>

                      <div className="event-header-right">
                        {canReplay && (
                          <button
                            type="button"
                            className="event-replay-button"
                            onClick={() => handleReplayEvent(event.eventId)}
                            disabled={isReplaying}
                            title="Replay this failed/DLQ webhook event"
                          >
                            {isReplaying ? (
                              <>
                                <span className="replay-spinner" />
                                <span>Replaying...</span>
                              </>
                            ) : (
                              <>
                                <svg
                                  className="replay-icon"
                                  width="14"
                                  height="14"
                                  viewBox="0 0 24 24"
                                  fill="none"
                                  stroke="currentColor"
                                  strokeWidth="2.2"
                                  strokeLinecap="round"
                                  strokeLinejoin="round"
                                >
                                  <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8" />
                                  <path d="M21 3v5h-5" />
                                  <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16" />
                                  <path d="M8 16H3v5" />
                                </svg>
                                <span>Replay</span>
                              </>
                            )}
                          </button>
                        )}
                      </div>

                    </div>


                    <div className="event-section">

                      <h3>Payload</h3>

                      <pre>
                        {event.payload}
                      </pre>

                    </div>


                    <div className="event-section">

                      <h3>Headers</h3>

                      <pre>
                        {JSON.stringify(
                          event.headers,
                          null,
                          2
                        )}
                      </pre>

                    </div>

                  </div>
                );
              })}

            </div>

          )}

        </div>

      </section>

    </main>
  );
}

export default EndpointDashboard;