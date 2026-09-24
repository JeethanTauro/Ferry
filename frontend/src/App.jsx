import { BrowserRouter, Routes, Route } from "react-router-dom";

import Navbar from "./components/Navbar/Navbar";

import Landing from "./pages/Landing/Landing";
import Login from "./pages/Login/Login";
import Signup from "./pages/Signup/Signup";
import Endpoints from "./pages/Endpoints/Endpoints";
import EndpointDashboard from "./pages/EndpointDashboard/EndpointDashboard";
import About from "./pages/About/About.jsx";
import ProtectedRoute from "./components/ProtectedRoute/ProtectedRoute.jsx";
function App() {
  return (
    <BrowserRouter>

      <Navbar />
    <Routes>
  {/* Public routes */}

  <Route
    path="/"
    element={<Landing />}
  />

  <Route
    path="/about"
    element={<About />}
  />

  <Route
    path="/login"
    element={<Login />}
  />

  <Route
    path="/signup"
    element={<Signup />}
  />


  {/* Protected routes */}

  <Route element={<ProtectedRoute />}>

    <Route
      path="/endpoints"
      element={<Endpoints />}
    />

    <Route
      path="/endpoints/:id"
      element={<EndpointDashboard />}
    />

  </Route>

</Routes>

    </BrowserRouter>
  );
}

export default App;