import { useEffect, useMemo, useState } from 'react';
import { Pagination, Spinner } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { applicationsApi, extractErrorMessage } from '../common/api';
import { useAuth } from '../auth/AuthContext';
import { getApplicationStatusLabel } from '../common/statuses';

const DASHBOARD_PAGE_SIZE = 10;

const getStatusClass = (status) =>
  ({
    PENDING: 'pending',
    CONFIRMED: 'confirmed',
    COMPLETED: 'completed',
    CANCELLED: 'cancelled',
    EXPIRED: 'expired',
  }[status] || 'default');

const getGreetingName = (fullName = '') => {
  const parts = fullName.split(' ').filter(Boolean);

  if (parts.length >= 3) {
    return parts.slice(1).join(' ');
  }

  if (parts.length >= 2) {
    return parts.slice(-2).join(' ');
  }

  return fullName || 'пользователь';
};

function UserDashboard() {
  const { user } = useAuth();
  const [applications, setApplications] = useState([]);
  const [latestPage, setLatestPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    const loadApplications = async () => {
      if (!user?.passportNumber) {
        setLoading(false);
        return;
      }

      try {
        const result = await applicationsApi.getByPassport(user.passportNumber);
        if (!cancelled) {
          setApplications(result);
        }
      } catch (loadError) {
        if (!cancelled) {
          setError(extractErrorMessage(loadError, 'Не удалось загрузить заявления'));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadApplications();

    return () => {
      cancelled = true;
    };
  }, [user?.passportNumber]);

  const recentApplications = useMemo(
    () =>
      [...applications].sort((left, right) => new Date(right.submissionDate || 0) - new Date(left.submissionDate || 0)),
    [applications],
  );

  const activeApplication = useMemo(
    () =>
      recentApplications.find((application) =>
        ['CONFIRMED', 'COMPLETED'].includes(application.status) && application.licensePlateNumber,
      ) || null,
    [recentApplications],
  );

  const summary = useMemo(
    () => ({
      total: applications.length,
      pending: applications.filter((application) => application.status === 'PENDING').length,
      completed: applications.filter((application) => application.status === 'COMPLETED').length,
      activePlate: activeApplication?.licensePlateNumber || '',
    }),
    [activeApplication, applications],
  );

  const paginatedRecentApplications = useMemo(() => {
    const start = (latestPage - 1) * DASHBOARD_PAGE_SIZE;
    return recentApplications.slice(start, start + DASHBOARD_PAGE_SIZE);
  }, [latestPage, recentApplications]);

  const latestTotalPages = Math.max(Math.ceil(recentApplications.length / DASHBOARD_PAGE_SIZE), 1);

  useEffect(() => {
    setLatestPage(1);
  }, [recentApplications.length]);

  if (loading) {
    return (
      <div className="loading-state">
        <Spinner />
      </div>
    );
  }

  const ActivePlateTag = activeApplication ? Link : 'article';
  const activePlateProps = activeApplication ? { to: `/applications?applicationId=${activeApplication.id}` } : {};

  return (
    <>
      <section className="page-header">
        <div className="page-title">
          <p className="eyebrow">Личный кабинет</p>
          <h1>Здравствуйте, {getGreetingName(user?.fullName)}</h1>
          <p>Все ваши заявления, статусы и данные в одном месте.</p>
        </div>
      </section>

      {error ? (
        <section className="info-banner">
          <i className="bi bi-info-circle" />
          <div>{error}</div>
        </section>
      ) : null}

      <section className="summary-grid">
        <article className="summary-card">
          <div className="detail-label">Всего заявлений</div>
          <div className="stat-value">{summary.total}</div>
        </article>
        <article className="summary-card">
          <div className="detail-label">На рассмотрении</div>
          <div className="stat-value">{summary.pending}</div>
        </article>
        <article className="summary-card">
          <div className="detail-label">Завершено</div>
          <div className="stat-value">{summary.completed}</div>
        </article>
        <ActivePlateTag
          className={`summary-card ${activeApplication ? 'summary-card-link' : ''}`}
          {...activePlateProps}
        >
          <div className="detail-label">Активный номер</div>
          <div className="stat-value plate-compact">{summary.activePlate || 'Нет'}</div>
          {activeApplication ? <div className="summary-card-hint">Открыть заявление №{activeApplication.id}</div> : null}
        </ActivePlateTag>
      </section>

      <section className="list-card">
        <div className="section-title-row">
          <h2 className="section-title">Последние заявления</h2>
        </div>
        <div className="application-showcase-list application-showcase-list-spacious">
          {paginatedRecentApplications.length ? (
            paginatedRecentApplications.map((application) => (
              <Link
                key={application.id}
                to={`/applications?applicationId=${application.id}`}
                className="application-showcase-card application-showcase-link application-showcase-static user-dashboard-application-link"
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
                      <span className="detail-label">Отделение</span>
                      <strong>{application.departmentName || 'Будет определено при обработке'}</strong>
                    </div>
                    <div>
                      <span className="detail-label">Дата подачи</span>
                      <strong>{application.submissionDate || 'Не указана'}</strong>
                    </div>
                  </div>
                </div>
              </Link>
            ))
          ) : (
            <div className="empty-state compact-empty">
              <i className="bi bi-journal-text" />
              <p>У вас пока нет заявлений.</p>
            </div>
          )}
        </div>

        {recentApplications.length > DASHBOARD_PAGE_SIZE ? (
          <div className="pagination-bar">
            <span className="helper-text">
              Показано {paginatedRecentApplications.length} из {recentApplications.length}
            </span>
            <Pagination className="mb-0">
              <Pagination.Prev
                disabled={latestPage === 1}
                onClick={() => setLatestPage((current) => Math.max(current - 1, 1))}
              />
              {Array.from({ length: latestTotalPages }).map((_, index) => (
                <Pagination.Item
                  key={index + 1}
                  active={latestPage === index + 1}
                  onClick={() => setLatestPage(index + 1)}
                >
                  {index + 1}
                </Pagination.Item>
              ))}
              <Pagination.Next
                disabled={latestPage === latestTotalPages}
                onClick={() => setLatestPage((current) => Math.min(current + 1, latestTotalPages))}
              />
            </Pagination>
          </div>
        ) : null}
      </section>
    </>
  );
}

export default UserDashboard;
