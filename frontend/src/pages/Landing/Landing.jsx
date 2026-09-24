import { Link } from "react-router-dom";
import ferryLogo from "../../assets/ferry-logo.png";
import "./Landing.css";

function Landing() {
  return (
    <main>

      {/* Hero */}
      <section id="home" className="hero">
        <div className="hero-content">

          <span className="hero-label">
            Reliable webhook infrastructure
          </span>

          <h1>
            Who's looking after
            <span> your webhook events?</span>
          </h1>

          <p className="hero-hook">
            You? C'mon. You already lost a few.
          </p>

          <p className="hero-description">
            Ferry makes webhook delivery reliable, observable,
            and recoverable. Let us handle the infrastructure
            while you focus on your application.
          </p>

          <div className="hero-actions">
            <Link to="/signup" className="primary-button">
              Get Started
            </Link>

            <a href="#about" className="secondary-button">
              Learn More
            </a>
          </div>

        </div>

        <div className="hero-visual">
          <div className="hero-logo-container">
            <img src={ferryLogo} alt="Ferry webhook infrastructure" />
          </div>
        </div>
      </section>


      {/* About Ferry */}
      <section id="about" className="about-section">

        <div className="section-heading">
          <span className="section-label">
            Why Ferry?
          </span>

          <h2>
            Webhook delivery shouldn't be
            <span> your problem.</span>
          </h2>

          <p>
            Your application shouldn't have to build and maintain
            an entire delivery infrastructure just to make sure
            webhook events reach their destination.
          </p>
        </div>

        <div className="comparison">

          {/* Without Ferry */}
          <div className="comparison-card without-ferry">

            <div className="comparison-header">
              <span className="comparison-indicator"></span>
              <h3>Without Ferry</h3>
            </div>

            <p className="comparison-description">
              Your team has to build and maintain the infrastructure.
            </p>

            <ul>
              <li>Build webhook persistence</li>
              <li>Implement retry logic</li>
              <li>Track failed deliveries</li>
              <li>Build a dead-letter queue</li>
              <li>Implement event replay</li>
              <li>Build usage tracking</li>
              <li>Monitor delivery failures</li>
            </ul>

          </div>


          {/* With Ferry */}
          <div className="comparison-card with-ferry">

            <div className="comparison-header">
              <span className="comparison-indicator"></span>
              <h3>With Ferry</h3>
            </div>

            <p className="comparison-description">
              Ferry handles the delivery infrastructure for you.
            </p>

            <ul>
              <li>Durable webhook delivery</li>
              <li>Automatic retries</li>
              <li>Delivery observability</li>
              <li>Dead-letter queue</li>
              <li>Event recovery</li>
              <li>Usage tracking</li>
              <li>Delivery status monitoring</li>
            </ul>

          </div>

        </div>

      </section>

    </main>
  );
}

export default Landing;