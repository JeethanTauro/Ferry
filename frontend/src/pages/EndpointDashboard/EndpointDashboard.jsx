import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../../context/AuthContext.jsx";

import { getEndpoint } from "../../services/api.js";

import "./EndpointDashboard.css";

function EndpointDashboard() {

  const { id } = useParams();

  const [endpoint, setEndpoint] = useState(null);
  const [usage, setUsage] = useState(null);

  const { user, loading: authLoading } = useAuth();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");


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

        setEndpoint(data.webhookResponse);
        setUsage(data.webhookResponseUsage);

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

    </main>
  );
}

export default EndpointDashboard;