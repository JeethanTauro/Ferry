import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext.jsx";

import {
  getEndpoints,
  createEndpoint,
  disableEndpoint,
  enableEndpoint,
  deleteEndpoint,
} from "../../services/api.js";

import "./Endpoints.css";

function Endpoints() {

  const { user, loading: authLoading } = useAuth();

  const [endpoints, setEndpoints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [showCreateModal, setShowCreateModal] = useState(false);

  const [name, setName] = useState("");
  const [destinationUrl, setDestinationUrl] = useState("");

  const [actionLoading, setActionLoading] = useState(false);

  const [createdSecret, setCreatedSecret] = useState("");
  const [createdEndpointId, setCreatedEndpointId] = useState("");
  const [showSecretModal, setShowSecretModal] = useState(false);


  const handleCopySecret = async () => {
    try {
      await navigator.clipboard.writeText(createdSecret);
    } catch (error) {
      console.error("Failed to copy secret:", error);
    }
  };


  // Delete
  const handleDeleteEndpoint = async (endpointId) => {

    const confirmed = window.confirm(
      "Are you sure you want to delete this endpoint?"
    );

    if (!confirmed) {
      return;
    }

    try {

      setActionLoading(true);
      setError("");

      if (!user) {
        throw new Error("You are not authenticated.");
      }

      const idToken = await user.getIdToken();

      await deleteEndpoint(
        idToken,
        endpointId
      );

      setEndpoints((currentEndpoints) =>
        currentEndpoints.filter(
          (endpoint) =>
            endpoint.endpointId !== endpointId
        )
      );

    } catch (error) {

      console.error(error);

      setError(
        error.message || "Failed to delete endpoint."
      );

    } finally {

      setActionLoading(false);

    }
  };


  // Enable / Disable
  const handleToggleEndpoint = async (endpoint) => {

    try {

      setActionLoading(true);
      setError("");

      if (!user) {
        throw new Error("You are not authenticated.");
      }

      const idToken = await user.getIdToken();

      if (endpoint.active) {

        await disableEndpoint(
          idToken,
          endpoint.endpointId
        );

      } else {

        await enableEndpoint(
          idToken,
          endpoint.endpointId
        );

      }

      const data = await getEndpoints(idToken);

      setEndpoints(
        data.webhookResponseList || []
      );

    } catch (error) {

      console.error(error);

      setError(
        error.message || "Failed to update endpoint."
      );

    } finally {

      setActionLoading(false);

    }
  };


  // Create
  const handleCreateEndpoint = async (event) => {

    event.preventDefault();

    try {

      setActionLoading(true);
      setError("");

      if (!user) {
        throw new Error("You are not authenticated.");
      }

      const idToken = await user.getIdToken();

      const createdEndpoint = await createEndpoint(
        idToken,
        {
          name,
          destinationUrl,
          provider: "GITHUB",
        }
      );

      setCreatedSecret(
        createdEndpoint.secretToken
      );

      /*
       * Keep this as createdEndpoint.endpoint
       * because your current create response
       * returns the public endpoint URL.
       */
      setCreatedEndpointId(
        createdEndpoint.endpoint
      );

      setName("");
      setDestinationUrl("");
      setShowCreateModal(false);
      setShowSecretModal(true);

      console.log(
        "Endpoint created:",
        createdEndpoint
      );

      const data = await getEndpoints(idToken);

      setEndpoints(
        data.webhookResponseList || []
      );

    } catch (error) {

      console.error(error);

      setError(
        error.message || "Failed to create endpoint."
      );

    } finally {

      setActionLoading(false);

    }
  };


  // Load endpoints
  useEffect(() => {

    if (authLoading) {
      return;
    }

    if (!user) {
      setError("You are not authenticated.");
      setLoading(false);
      return;
    }

    const loadEndpoints = async () => {

      try {

        const idToken = await user.getIdToken();

        const data = await getEndpoints(idToken);

        setEndpoints(
          data.webhookResponseList || []
        );

      } catch (error) {

        console.error(error);

        setError(
          "Failed to load your webhook endpoints."
        );

      } finally {

        setLoading(false);

      }
    };

    loadEndpoints();

  }, [user, authLoading]);


  return (
    <main className="endpoints-page">

      <section className="endpoints-header">

        <div>

          <span className="section-label">
            WEBHOOKS
          </span>

          <h1>Your Endpoints</h1>

          <p>
            Manage your webhook endpoints and monitor
            their delivery activity.
          </p>

        </div>

        <button
          className="create-endpoint-button"
          onClick={() => setShowCreateModal(true)}
        >
          + Create Endpoint
        </button>

      </section>


      <section className="endpoints-content">

        {loading && (
          <div className="endpoints-state">
            <p>Loading your endpoints...</p>
          </div>
        )}


        {!loading && error && (
          <div className="endpoints-state error-state">
            <p>{error}</p>
          </div>
        )}


        {!loading &&
          !error &&
          endpoints.length === 0 && (

            <div className="endpoints-state empty-state">

              <h2>No endpoints yet</h2>

              <p>
                You haven't created any webhook endpoints yet.
              </p>

              <button
                className="create-endpoint-button"
                onClick={() => setShowCreateModal(true)}
              >
                Create your first endpoint
              </button>

            </div>
          )}


        {!loading &&
          !error &&
          endpoints.length > 0 && (

            <div className="endpoint-list">

              {endpoints.map((endpoint) => (

                <div
                  className="endpoint-card"
                  key={endpoint.endpointId}
                >

                  <div className="endpoint-card-top">

                    <div className="endpoint-info">

                      <div className="endpoint-title-row">

                        <h2>
                          {endpoint.name}
                        </h2>

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

                      <p className="endpoint-url">
                        {endpoint.endpoint}
                      </p>

                    </div>

                    <div className="provider-badge">
                      GitHub
                    </div>

                  </div>


                  <div className="endpoint-details">

                    <div className="endpoint-detail">

                      <span className="detail-label">
                        Destination
                      </span>

                      <span className="detail-value">
                        {endpoint.destinationUrl}
                      </span>

                    </div>


                    <div className="endpoint-detail">

                      <span className="detail-label">
                        Rate Limit
                      </span>

                      <span className="detail-value">
                        {endpoint.rateLimit}
                        {" / sec"}
                      </span>

                    </div>


                    <div className="endpoint-detail">

                      <span className="detail-label">
                        Created
                      </span>

                      <span className="detail-value">
                        {new Date(
                          endpoint.createdAt
                        ).toLocaleDateString()}
                      </span>

                    </div>

                  </div>


                  <div className="endpoint-card-footer">

                    <span className="endpoint-status">

                      <span
                        className={`status-dot ${
                          endpoint.active
                            ? ""
                            : "inactive-dot"
                        }`}
                      />

                      {endpoint.active
                        ? "Receiving events"
                        : "Endpoint disabled"}

                    </span>


                    <div className="endpoint-actions">

                      <button
                        className="endpoint-action-button"
                        onClick={() =>
                          handleToggleEndpoint(endpoint)
                        }
                        disabled={actionLoading}
                      >
                        {endpoint.active
                          ? "Disable"
                          : "Enable"}
                      </button>


                      <button
                        className="endpoint-action-button delete"
                        onClick={() =>
                          handleDeleteEndpoint(
                            endpoint.endpointId
                          )
                        }
                        disabled={actionLoading}
                      >
                        Delete
                      </button>


                      <Link
                        to={`/endpoints/${endpoint.endpointId}`}
                        className="view-endpoint-button"
                      >
                        View Dashboard →
                      </Link>

                    </div>

                  </div>

                </div>

              ))}

            </div>
          )}

      </section>


      {/* Create Modal */}

      {showCreateModal && (

        <div className="modal-overlay">

          <div className="create-modal">

            <div className="modal-header">

              <div>

                <span className="section-label">
                  NEW ENDPOINT
                </span>

                <h2>Create Endpoint</h2>

              </div>

              <button
                className="modal-close"
                onClick={() =>
                  setShowCreateModal(false)
                }
              >
                ×
              </button>

            </div>


            <form
              className="create-endpoint-form"
              onSubmit={handleCreateEndpoint}
            >

              <div className="form-group">

                <label htmlFor="endpoint-name">
                  Name
                </label>

                <input
                  id="endpoint-name"
                  type="text"
                  placeholder="GitHub Production"
                  value={name}
                  onChange={(event) =>
                    setName(event.target.value)
                  }
                  required
                />

              </div>


              <div className="form-group">

                <label htmlFor="destination-url">
                  Destination URL
                </label>

                <input
                  id="destination-url"
                  type="url"
                  placeholder="https://your-api.com/webhooks"
                  value={destinationUrl}
                  onChange={(event) =>
                    setDestinationUrl(event.target.value)
                  }
                  required
                />

              </div>


              <div className="form-group">

                <label htmlFor="provider">
                  Provider
                </label>

                <select
                  id="provider"
                  value="GITHUB"
                  disabled
                >
                  <option value="GITHUB">
                    GitHub
                  </option>
                </select>

              </div>


              <div className="modal-actions">

                <button
                  type="button"
                  className="cancel-button"
                  onClick={() =>
                    setShowCreateModal(false)
                  }
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="create-endpoint-button"
                  disabled={actionLoading}
                >
                  {actionLoading
                    ? "Creating..."
                    : "Create Endpoint"}
                </button>

              </div>

            </form>

          </div>

        </div>

      )}


      {/* Secret Modal */}

      {showSecretModal && (

        <div className="modal-overlay">

          <div className="secret-modal">

            <div className="secret-modal-header">

              <span className="section-label">
                ENDPOINT CREATED
              </span>

              <h2>Save your secret token</h2>

              <p>
                Your endpoint has been created successfully.
                Store this secret somewhere secure. You will need
                it to verify webhook requests sent to your endpoint.
              </p>

            </div>


            <div className="secret-warning">

              <strong>Important:</strong>

              <span>
                This secret should be treated like a password.
                Do not share it publicly or commit it to your source code.
              </span>

            </div>


            <div className="secret-container">

              <code>
                {createdSecret}
              </code>

              <button
                className="copy-secret-button"
                onClick={handleCopySecret}
              >
                Copy Secret
              </button>

            </div>


            <p className="secret-note">
              Make sure you have stored this secret before closing
              this window.
            </p>


            <button
              className="secret-done-button"
              onClick={() => {
                setShowSecretModal(false);
                setCreatedSecret("");
                setCreatedEndpointId("");
              }}
            >
              I've saved the secret
            </button>

          </div>

        </div>

      )}

    </main>
  );
}

export default Endpoints;