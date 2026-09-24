import "./About.css";

function About() {
  return (
    <main className="about-page">

      <section className="about-hero">
        <span className="section-label">ABOUT FERRY</span>

        <h1>
          Webhook delivery,
          <span> without the headache.</span>
        </h1>

        <p>
          Ferry is an asynchronous webhook delivery platform that
          handles the difficult parts of receiving, persisting,
          delivering, and recovering webhook events.
        </p>
      </section>


      <section className="about-section">

        <div className="about-section-heading">
          <span className="section-label">WHAT FERRY DOES</span>

          <h2>Your webhook infrastructure, handled.</h2>

          <p>
            Instead of building persistence, retries, failure handling,
            and delivery tracking yourself, Ferry provides these
            capabilities as part of the delivery pipeline.
          </p>
        </div>

        <div className="about-flow">

          <div className="flow-card">
            <span>01</span>
            <h3>Receive</h3>
            <p>
              Ferry receives webhook events from your providers and
              verifies the incoming request.
            </p>
          </div>

          <div className="flow-card">
            <span>02</span>
            <h3>Persist</h3>
            <p>
              Events are durably stored in PostgreSQL before they
              continue through the delivery pipeline.
            </p>
          </div>

          <div className="flow-card">
            <span>03</span>
            <h3>Deliver</h3>
            <p>
              Events are published through RabbitMQ and delivered
              asynchronously by the delivery worker.
            </p>
          </div>

          <div className="flow-card">
            <span>04</span>
            <h3>Recover</h3>
            <p>
              Failed deliveries can be retried, and events that
              exhaust their retry attempts are moved to the DLQ.
            </p>
          </div>

        </div>

      </section>


      <section className="about-section about-reliability">

        <div className="about-section-heading">
          <span className="section-label">WHY IT MATTERS</span>

          <h2>
            Your application shouldn't have to build all of this.
          </h2>
        </div>

        <div className="reliability-grid">

          <div>
            <h3>Durable persistence</h3>
            <p>
              Accepted webhook events are persisted before they
              continue into the messaging pipeline.
            </p>
          </div>

          <div>
            <h3>Automatic retries</h3>
            <p>
              Transient failures such as 429 responses, 5xx responses,
              and network failures are retried with backoff.
            </p>
          </div>

          <div>
            <h3>Dead-letter handling</h3>
            <p>
              Events that exhaust their retry attempts are moved
              to the dead-letter queue instead of being retried forever.
            </p>
          </div>

          <div>
            <h3>Observability</h3>
            <p>
              Events can be traced through their lifecycle using
              identifiers such as event ID, endpoint ID, retry count,
              destination, and HTTP status.
            </p>
          </div>

        </div>

      </section>


      <section className="about-section about-how">

        <div className="about-section-heading">
          <span className="section-label">UNDER THE HOOD</span>

          <h2>How Ferry works</h2>

          <p>
            Ferry separates webhook ingestion from delivery so that
            receiving an event does not depend on the destination
            being immediately available.
          </p>
        </div>

        <div className="simple-architecture">

          <div>Webhook Provider</div>
          <span>→</span>
          <div>Ferry Ingestion</div>
          <span>→</span>
          <div>PostgreSQL</div>
          <span>→</span>
          <div>RabbitMQ</div>
          <span>→</span>
          <div>Delivery Worker</div>
          <span>→</span>
          <div>Your API</div>

        </div>

      </section>

    </main>
  );
}

export default About;