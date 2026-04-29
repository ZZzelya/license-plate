import { useState } from 'react';
import { Dropdown } from 'react-bootstrap';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const buildInitials = (name = '') =>
  name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((item) => item[0]?.toUpperCase())
    .join('') || 'ЛК';

function Navbar() {
  const { user, role, theme, toggleTheme, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  const adminPrimaryNavItems = [
    { to: '/admin', icon: 'bi-speedometer2', label: 'Панель' },
    { to: '/admin/applicants', icon: 'bi-people', label: 'Заявители' },
    { to: '/admin/applications', icon: 'bi-journal-text', label: 'Заявления' },
  ];

  const adminSecondaryNavItems = [
    { to: '/admin/departments', icon: 'bi-buildings', label: 'Отделения' },
    { to: '/admin/license-plates', icon: 'bi-credit-card-2-front', label: 'Номерные знаки' },
    { to: '/admin/services', icon: 'bi-stars', label: 'Услуги' },
  ];

  const userNavItems = [
    { to: '/profile', icon: 'bi-person-badge', label: 'Мой профиль' },
    { to: '/applications', icon: 'bi-journal-text', label: 'Мои заявления' },
  ];

  const displayName = user?.fullName || (role === 'ADMIN' ? 'Администратор' : 'Пользователь');
  const userHint =
    role === 'ADMIN'
      ? user?.username || 'admin'
      : user?.email || user?.passportNumber || user?.username;

  if (role === 'USER') {
    return (
      <header className="topbar glass-panel topbar-admin topbar-user">
        <div className="brand-block brand-block-user-center brand-block-admin">
          <NavLink to="/" className="brand-link brand-link-user-centered brand-link-admin-centered">
            <span className="brand-logo brand-logo-hero">
              <i className="bi bi-car-front-fill" />
            </span>
            <div>
              <strong>Автомобильные номерные знаки</strong>
              <small>Система получения</small>
            </div>
          </NavLink>
        </div>

        <div className="admin-nav-stack">
          <nav className="nav-links nav-links-admin nav-links-admin-primary">
            {userNavItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => `nav-pill ${isActive ? 'active' : ''}`}
              >
                <i className={`bi ${item.icon}`} />
                <span>{item.label}</span>
              </NavLink>
            ))}
          </nav>
        </div>

        <div className="topbar-actions">
          <button type="button" className="icon-button" onClick={toggleTheme} title="Переключить тему">
            <i className={`bi ${theme === 'dark' ? 'bi-sunrise' : 'bi-moon-stars'}`} />
          </button>

          <Dropdown align="end" show={menuOpen} onToggle={(nextShow) => setMenuOpen(Boolean(nextShow))}>
            <Dropdown.Toggle as="button" className={`user-badge ${menuOpen ? 'is-open' : ''}`}>
              <span className="avatar-circle">{buildInitials(displayName)}</span>
              <div className="user-meta">
                <strong>{displayName}</strong>
                <small>Личный кабинет</small>
              </div>
              <i className={`bi ${menuOpen ? 'bi-chevron-up' : 'bi-chevron-down'} user-badge-chevron`} />
            </Dropdown.Toggle>

            <Dropdown.Menu className="glass-dropdown">
              <Dropdown.ItemText>
                <div className="dropdown-user">
                  <strong>{userHint}</strong>
                  <small>Личный кабинет пользователя</small>
                </div>
              </Dropdown.ItemText>
              <Dropdown.Divider />
              {userNavItems.map((item) => (
                <Dropdown.Item key={item.to} as={NavLink} to={item.to} className="dropdown-link">
                  <i className={`bi ${item.icon} me-2`} />
                  {item.label}
                </Dropdown.Item>
              ))}
              <Dropdown.Divider />
              <Dropdown.Item onClick={logout} className="dropdown-link dropdown-link-danger">
                <i className="bi bi-box-arrow-right me-2" />
                Выйти
              </Dropdown.Item>
            </Dropdown.Menu>
          </Dropdown>
        </div>
      </header>
    );
  }

  return (
    <header className="topbar glass-panel topbar-admin">
      <div className="brand-block brand-block-user-center brand-block-admin">
        <NavLink to="/admin" className="brand-link brand-link-user-centered brand-link-admin-centered">
          <span className="brand-logo brand-logo-hero">
            <i className="bi bi-car-front-fill" />
          </span>
          <div>
            <strong>Автомобильные номерные знаки</strong>
            <small>Система получения</small>
          </div>
        </NavLink>
      </div>

      <div className="admin-nav-stack">
        <nav className="nav-links nav-links-admin nav-links-admin-primary">
          {adminPrimaryNavItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `nav-pill ${isActive ? 'active' : ''}`}
            >
              <i className={`bi ${item.icon}`} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <nav className="nav-links nav-links-admin nav-links-admin-secondary">
          {adminSecondaryNavItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `nav-pill ${isActive ? 'active' : ''}`}
            >
              <i className={`bi ${item.icon}`} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </div>

      <div className="topbar-actions">
        <button type="button" className="icon-button" onClick={toggleTheme} title="Переключить тему">
          <i className={`bi ${theme === 'dark' ? 'bi-sunrise' : 'bi-moon-stars'}`} />
        </button>

        <Dropdown align="end" show={menuOpen} onToggle={(nextShow) => setMenuOpen(Boolean(nextShow))}>
          <Dropdown.Toggle as="button" className={`user-badge ${menuOpen ? 'is-open' : ''}`}>
            <span className="avatar-circle">{buildInitials(displayName)}</span>
            <div className="user-meta">
              <strong>{displayName}</strong>
            </div>
            <i className={`bi ${menuOpen ? 'bi-chevron-up' : 'bi-chevron-down'} user-badge-chevron`} />
          </Dropdown.Toggle>

          <Dropdown.Menu className="glass-dropdown">
            <Dropdown.ItemText>
              <div className="dropdown-user">
                <strong>{userHint}</strong>
                <small>Панель управления системой</small>
              </div>
            </Dropdown.ItemText>
            <Dropdown.Divider />
            <Dropdown.Item as={NavLink} to="/admin" className="dropdown-link">
              <i className="bi bi-speedometer2 me-2" />
              Панель
            </Dropdown.Item>
            <Dropdown.Item as={NavLink} to="/admin/applicants" className="dropdown-link">
              <i className="bi bi-people me-2" />
              Заявители
            </Dropdown.Item>
            <Dropdown.Item as={NavLink} to="/admin/applications" className="dropdown-link">
              <i className="bi bi-journal-text me-2" />
              Заявления
            </Dropdown.Item>
            <Dropdown.Item as={NavLink} to="/admin/departments" className="dropdown-link">
              <i className="bi bi-buildings me-2" />
              Отделения
            </Dropdown.Item>
            <Dropdown.Item as={NavLink} to="/admin/license-plates" className="dropdown-link">
              <i className="bi bi-credit-card-2-front me-2" />
              Номерные знаки
            </Dropdown.Item>
            <Dropdown.Item as={NavLink} to="/admin/services" className="dropdown-link">
              <i className="bi bi-stars me-2" />
              Услуги
            </Dropdown.Item>
            <Dropdown.Divider />
            <Dropdown.Item onClick={logout} className="dropdown-link dropdown-link-danger">
              <i className="bi bi-box-arrow-right me-2" />
              Выйти
            </Dropdown.Item>
          </Dropdown.Menu>
        </Dropdown>
      </div>
    </header>
  );
}

export default Navbar;
