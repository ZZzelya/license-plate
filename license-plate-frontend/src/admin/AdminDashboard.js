import { useEffect, useMemo, useState } from 'react';
import { Button, Spinner } from 'react-bootstrap';
import { NavLink } from 'react-router-dom';
import { applicantsApi, applicationsApi, departmentsApi, licensePlatesApi } from '../common/api';
import { getApplicationStatusLabel } from '../common/statuses';

const formatShortDate = (value) =>
  new Intl.DateTimeFormat('ru-RU', { day: '2-digit', month: '2-digit' }).format(new Date(value));

const getStatusClass = (status) =>
  ({
    PENDING: 'pending',
    CONFIRMED: 'confirmed',
    COMPLETED: 'completed',
    CANCELLED: 'cancelled',
    EXPIRED: 'expired',
  }[status] || 'default');

const buildInitials = (name = '') =>
  name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('') || 'ЗЯ';

function AdminDashboard() {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    applicants: [],
    applications: [],
    departments: [],
    plates: [],
  });

  useEffect(() => {
    let cancelled = false;

    const loadData = async () => {
      setLoading(true);

      try {
        const [applicants, applications, departments, plates] = await Promise.all([
          applicantsApi.getAll(),
          applicationsApi.getAll(),
          departmentsApi.getAll(),
          licensePlatesApi.getAll(),
        ]);

        if (!cancelled) {
          setStats({ applicants, applications, departments, plates });
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadData();

    return () => {
      cancelled = true;
    };
  }, []);

  const chartData = useMemo(() => {
    const days = [...Array.from({ length: 7 }).keys()].map((offset) => {
      const date = new Date();
      date.setDate(date.getDate() - (6 - offset));
      const key = date.toISOString().slice(0, 10);
      const count = stats.applications.filter(
        (application) => application.submissionDate?.slice(0, 10) === key,
      ).length;

      return {
        label: formatShortDate(date),
        count,
      };
    });

    const max = Math.max(...days.map((item) => item.count), 1);

    return days.map((item) => ({
      ...item,
      height: `${Math.max((item.count / max) * 100, item.count ? 18 : 8)}%`,
    }));
  }, [stats.applications]);

  const latestApplications = useMemo(
    () =>
      [...stats.applications]
        .sort((left, right) => new Date(right.submissionDate) - new Date(left.submissionDate))
        .slice(0, 5),
    [stats.applications],
  );

  const topApplicants = useMemo(
    () =>
      [...stats.applicants]
        .sort((left, right) => (right.applicationsCount || 0) - (left.applicationsCount || 0))
        .slice(0, 3),
    [stats.applicants],
  );

  if (loading) {
    return (
      <div className="loading-state">
        <Spinner />
      </div>
    );
  }

  return (
    <>
      <section className="page-header">
        <div className="page-title">
          <p className="eyebrow">Администрирование</p>
          <h1>Общая сводка системы</h1>
          <p>Здесь видно, как движутся заявления, кто подает чаще и как распределены ресурсы по отделениям.</p>
        </div>
      </section>

      <section className="stats-grid">
        {[
          {
            icon: 'bi-journal-text',
            title: 'Всего заявлений',
            value: stats.applications.length,
            caption: 'С учетом всех статусов',
            to: '/admin/applications',
          },
          {
            icon: 'bi-people',
            title: 'Заявители',
            value: stats.applicants.length,
            caption: 'Активные пользователи системы',
            to: '/admin/applicants',
          },
          {
            icon: 'bi-credit-card-2-front',
            title: 'Номерные знаки',
            value: stats.plates.length,
            caption: 'В каталоге системы',
            to: '/admin/license-plates',
          },
          {
            icon: 'bi-buildings',
            title: 'Отделения',
            value: stats.departments.length,
            caption: 'Подключенные подразделения',
            to: '/admin/departments',
          },
        ].map((item) => (
          <NavLink key={item.title} to={item.to} className="metric-card metric-link-card">
            <div className="metric-icon">
              <i className={`bi ${item.icon}`} />
            </div>
            <h3>{item.title}</h3>
            <div className="stat-value">{item.value}</div>
            <div className="stat-caption">{item.caption}</div>
          </NavLink>
        ))}
      </section>

      <section className="chart-grid">
        <article className="chart-card">
          <div className="section-title-row">
            <h2 className="section-title">Заявления за последние 7 дней</h2>
          </div>
          <div className="chart-bars">
            {chartData.map((item) => (
              <div key={item.label} className="chart-bar-column">
                <div className="chart-value">{item.count}</div>
                <div className="chart-bar" style={{ height: item.height }} />
                <div className="chart-label">{item.label}</div>
              </div>
            ))}
          </div>
        </article>

        <article className="list-card">
          <div className="section-title-row">
            <h2 className="section-title">Топ-3 заявителя</h2>
          </div>
          <div className={`mini-list top-applicants-list top-applicants-count-${Math.max(topApplicants.length, 1)}`}>
            {topApplicants.map((applicant) => (
              <NavLink
                key={applicant.id}
                to={`/admin/applicants?applicantId=${applicant.id}`}
                className="mini-list-item mini-list-link applicant-mini-link"
              >
                <div className="mini-list-identity">
                  <span className="mini-avatar-badge">{buildInitials(applicant.fullName)}</span>
                  <div className="mini-list-copy">
                    <strong>{applicant.fullName}</strong>
                    <small>{applicant.passportNumber}</small>
                  </div>
                </div>
                <span className="soft-badge">{applicant.applicationsCount || 0} заявл.</span>
              </NavLink>
            ))}
          </div>
        </article>
      </section>

      <section className="list-card">
        <div className="section-title-row">
          <h2 className="section-title">Последние 5 заявлений</h2>
          <Button as={NavLink} to="/admin/applications" className="primary-button">
            Все заявления
          </Button>
        </div>

        <div className="application-showcase-list">
          {latestApplications.map((application) => (
            <NavLink
              key={application.id}
              to={`/admin/applications?applicationId=${application.id}`}
              className="application-showcase-card application-showcase-link application-showcase-static"
            >
              <div className="application-showcase-main">
                <div className="application-showcase-head">
                  <span className="application-id-badge">Заявление №{application.id}</span>
                  <span className={`status-pill ${getStatusClass(application.status)}`}>
                    {getApplicationStatusLabel(application.status)}
                  </span>
                </div>

                <div className="application-showcase-grid">
                  <div>
                    <span className="detail-label">Номерной знак</span>
                    <strong>{application.licensePlateNumber || 'Назначается системой'}</strong>
                  </div>
                  <div>
                    <span className="detail-label">Заявитель</span>
                    <strong>{application.applicantName || 'Не указан'}</strong>
                  </div>
                  <div>
                    <span className="detail-label">Дата подачи</span>
                    <strong>{application.submissionDate || 'Не указана'}</strong>
                  </div>
                </div>
              </div>
            </NavLink>
          ))}
        </div>
      </section>
    </>
  );
}

export default AdminDashboard;
