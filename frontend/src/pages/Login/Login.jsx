import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { signInWithEmailAndPassword } from "firebase/auth";

import { auth } from "../../services/firebase";
import { loginToFerry } from "../../services/api";

import "./Login.css";

function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleLogin = async (event) => {
    event.preventDefault();

    setError("");

    try {
      setLoading(true);

      // Authenticate with Firebase
      const userCredential =
        await signInWithEmailAndPassword(
          auth,
          email,
          password
        );

      // Get Firebase ID token
      const idToken =
        await userCredential.user.getIdToken();

      // Authenticate the user with Ferry
      await loginToFerry(idToken);

      // User is now authenticated with both Firebase and Ferry
      navigate("/endpoints");

    } catch (error) {
      console.error(error);

      setError(error.message);

    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <div className="login-container">

        <div className="login-header">
          <span className="login-label">WELCOME BACK</span>

          <h1>Log in to Ferry</h1>

          <p>
            Manage your webhook endpoints and delivery infrastructure.
          </p>
        </div>

        <form
          className="login-form"
          onSubmit={handleLogin}
        >

          <div className="form-group">
            <label htmlFor="email">
              Email
            </label>

            <input
              id="email"
              type="email"
              placeholder="Enter your email"
              value={email}
              onChange={(event) =>
                setEmail(event.target.value)
              }
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">
              Password
            </label>

            <input
              id="password"
              type="password"
              placeholder="Enter your password"
              value={password}
              onChange={(event) =>
                setPassword(event.target.value)
              }
              required
            />
          </div>

          {error && (
            <p className="login-error">
              {error}
            </p>
          )}

          <button
            type="submit"
            className="login-button"
            disabled={loading}
          >
            {loading ? "Logging in..." : "Log In"}
          </button>

        </form>

        <p className="signup-prompt">
          Don't have an account?{" "}
          <Link to="/signup">
            Sign up
          </Link>
        </p>

      </div>
    </main>
  );
}

export default Login;