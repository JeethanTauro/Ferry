import { Link } from "react-router-dom";
import "./Navbar.css";
import ferryLogo from "../../assets/ferry-logo.png";
import { useAuth } from "../../context/AuthContext.jsx";

function Navbar() {
    const { user, loading } = useAuth();
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
         <Link to="/about" className="navbar-about" >About</Link>
         
          {!loading && (
                <Link to={user ? "/endpoints" : "/login"}  className="navbar-endpoints">
                Endpoints
            </Link>
        )}

          <Link to="/login" className="navbar-login">
            Login
          </Link>

          <Link to="/signup" className="navbar-signup">
            Sign Up
          </Link>

         
        </div>

      </div>
    </header>
  );
}

export default Navbar;