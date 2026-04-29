import { Spinner } from 'react-bootstrap';
import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import AdminApplicants from './admin/AdminApplicants';
import AdminApplications from './admin/AdminApplications';
import AdminDashboard from './admin/AdminDashboard';
import AdminDepartments from './admin/AdminDepartments';
import AdminLicensePlates from './admin/AdminLicensePlates';
import AdminServices from './admin/AdminServices';
import Login from './auth/Login';
import Register from './auth/Register';
import LandingPage from './common/LandingPage';
import Navbar from './common/Navbar';
import UserApplications from './user/UserApplications';
import UserDashboard from './user/UserDashboard';
import UserProfile from './user/UserProfile';
import { useAuth } from './auth/AuthContext';
import './App.css';

function PrivateRoute({ allowedRoles }) {
  const { isAuthenticated, role, getDefaultRoute, authLoading } = useAuth();
  const location = useLocation();

  if (authLoading) {
    return (
      <div className="loading-state">
        <Spinner />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (allowedRoles?.length && !allowedRoles.includes(role)) {
    return <Navigate to={getDefaultRoute(role)} replace />;
  }

  return <Outlet />;
}

function GuestRoute({ children }) {
  const { isAuthenticated, getDefaultRoute, authLoading } = useAuth();

  if (authLoading) {
    return (
      <div className="loading-state">
        <Spinner />
      </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to={getDefaultRoute()} replace />;
  }

  return children;
}

function AppLayout() {
  return (
    <div className="app-shell">
      <div className="app-background-orb orb-left" />
      <div className="app-background-orb orb-right" />
      <Navbar />
      <main className="page-shell">
        <Outlet />
      </main>
    </div>
  );
}

function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route
        path="/login"
        element={
          <GuestRoute>
            <Login />
          </GuestRoute>
        }
      />
      <Route
        path="/register"
        element={
          <GuestRoute>
            <Register />
          </GuestRoute>
        }
      />

      <Route element={<PrivateRoute />}>
        <Route element={<AppLayout />}>
          <Route element={<PrivateRoute allowedRoles={['USER']} />}>
            <Route path="/dashboard" element={<UserDashboard />} />
            <Route path="/profile" element={<UserProfile />} />
            <Route path="/applications" element={<UserApplications />} />
          </Route>

          <Route element={<PrivateRoute allowedRoles={['ADMIN']} />}>
            <Route path="/admin" element={<AdminDashboard />} />
            <Route path="/admin/applicants" element={<AdminApplicants />} />
            <Route path="/admin/applications" element={<AdminApplications />} />
            <Route path="/admin/departments" element={<AdminDepartments />} />
            <Route path="/admin/license-plates" element={<AdminLicensePlates />} />
            <Route path="/admin/services" element={<AdminServices />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
