import { Link, useNavigate } from "react-router-dom";
import { signOut } from "firebase/auth";
import { auth } from "../../services/firebase";
import { useAuth } from "../../context/AuthContext.jsx";
import ferryLogo from "../../assets/ferry-logo.png";
import "./Navbar.css";

function Navbar() {
  const navigate = useNavigate();
  const { user, loading } = useAuth();

  const handleLogout = async () => {
    try {
      await signOut(auth);
      navigate("/login");
    } catch (error) {
      console.error("Failed to log out:", error);
    }
  };

  return (
    <header className="navbar">
      <div className="navbar-container">

        <Link to="/" className="navbar-logo">
          <img src={ferryLogo} alt="Ferry" />
        </Link>

        <nav className="navbar-links">
          <Link to="/" className="navbar-home">
            Home
          </Link>
        </nav>

        <div className="navbar-actions">
          <Link to="/about" className="navbar-about">
            About
          </Link>

          {!loading && (
            user ? (
              <>
                <Link to="/endpoints" className="navbar-endpoints">
                  Endpoints
                </Link>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="navbar-logout"
                >
                  Log Out
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="navbar-login">
                  Login
                </Link>

                <Link to="/signup" className="navbar-signup">
                  Sign Up
                </Link>
              </>
            )
          )}
        </div>

      </div>
    </header>
  );
}

export default Navbar;