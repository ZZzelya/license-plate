import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Form, Pagination, Spinner } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { useSearchParams } from 'react-router-dom';
import { applicationsApi, departmentsApi, extractErrorMessage, servicesApi } from '../common/api';
import { getApplicationStatusLabel } from '../common/statuses';
import { isAvailablePlateServiceRule } from '../common/plateRules';
import {
  ApplicationActionModal,
  ApplicationDetailsModal,
  ApplicationWizardModal,
} from '../components/ApplicationModals';
import { useAuth } from '../auth/AuthContext';

const PAGE_SIZE = 10;

const getStatusClass = (status) =>
  ({
    PENDING: 'pending',
    CONFIRMED: 'confirmed',
    COMPLETED: 'completed',
    CANCELLED: 'cancelled',
    EXPIRED: 'expired',
  }[status] || 'default');

function UserApplications() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [applications, setApplications] = useState([]);
  const [services, setServices] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [page, setPage] = useState(1);
  const [showWizard, setShowWizard] = useState(false);
  const [detailsApplication, setDetailsApplication] = useState(null);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [applicationNumberSearch, setApplicationNumberSearch] = useState('');
  const [sortDirection, setSortDirection] = useState('desc');

  const passportNumber = user?.passportNumber;
  const requestedApplicationId = searchParams.get('applicationId');

  const openDetails = async (applicationId) => {
    try {
      const result = await applicationsApi.getWithDetails(applicationId);
      setDetailsApplication(result);
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось открыть заявление'));
    }
  };

  const loadData = async () => {
    if (!passportNumber) {
      setLoading(false);
      return;
    }

    setLoading(true);

    try {
      const [applicationsResult, servicesResult, departmentsResult] = await Promise.all([
        applicationsApi.getByPassport(passportNumber),
        servicesApi.getAvailable(),
        departmentsApi.getAll(),
      ]);

      setApplications(applicationsResult);
      setServices(
        servicesResult.map((service) =>
          isAvailablePlateServiceRule(service)
            ? { ...service, description: 'Выбор из предложенных номеров' }
            : service,
        ),
      );
      setDepartments(departmentsResult);
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось загрузить данные'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [passportNumber]);

  useEffect(() => {
    if (!requestedApplicationId || loading) {
      return;
    }

    const targetId = Number(requestedApplicationId);
    if (!Number.isFinite(targetId)) {
      return;
    }

    if (!applications.some((application) => application.id === targetId)) {
      return;
    }

    openDetails(targetId);
    const nextParams = new URLSearchParams(searchParams);
    nextParams.delete('applicationId');
    setSearchParams(nextParams, { replace: true });
  }, [applications, loading, requestedApplicationId, searchParams, setSearchParams]);

  const filteredApplications = useMemo(() => {
    const normalizedSearch = applicationNumberSearch.replace(/[^\d]/g, '').trim();

    return applications.filter((application) =>
      normalizedSearch ? String(application.id).includes(normalizedSearch) : true,
    );
  }, [applicationNumberSearch, applications]);

  const sortedApplications = useMemo(
    () =>
      [...filteredApplications].sort((left, right) => {
        const leftDate = new Date(left.submissionDate || 0);
        const rightDate = new Date(right.submissionDate || 0);
        return sortDirection === 'desc' ? rightDate - leftDate : leftDate - rightDate;
      }),
    [filteredApplications, sortDirection],
  );

  const paginatedApplications = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return sortedApplications.slice(start, start + PAGE_SIZE);
  }, [page, sortedApplications]);

  const totalPages = Math.max(Math.ceil(sortedApplications.length / PAGE_SIZE), 1);

  const activePlate = useMemo(
    () =>
      applications.find((application) => ['CONFIRMED', 'COMPLETED'].includes(application.status))?.licensePlateNumber,
    [applications],
  );

  useEffect(() => {
    setPage(1);
  }, [applicationNumberSearch, sortDirection, sortedApplications.length]);

  const handleCreate = async (payload) => {
    setSubmitting(true);

    try {
      await applicationsApi.create(payload);
      toast.success('Заявление создано');
      setShowWizard(false);
      await loadData();
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось создать заявление'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = async (comment) => {
    if (!cancelTarget) {
      return;
    }

    setSubmitting(true);

    try {
      await applicationsApi.cancel(cancelTarget.id, { comment });
      toast.success('Заявление отменено');
      setCancelTarget(null);
      await loadData();
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось отменить заявление'));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="loading-state">
        <Spinner />
      </div>
    );
  }

  if (!passportNumber) {
    return (
      <section className="empty-state">
        <i className="bi bi-person-badge" />
        <p>В профиле не найден номер паспорта. Без него нельзя показать ваши заявления.</p>
      </section>
    );
  }

  return (
    <>
      <section className="page-header page-header-inline">
        <div className="page-title">
          <p className="eyebrow">Мои заявления</p>
          <h1>История ваших заявлений</h1>
        </div>
        <div className="page-actions">
          <Button className="primary-button" onClick={() => setShowWizard(true)}>
            <i className="bi bi-plus-circle me-2" />
            Новое заявление
          </Button>
        </div>
      </section>

      {activePlate ? (
        <Alert variant="warning" className="glass-alert warning-alert">
          <strong>У вас уже есть активный номерной знак: {activePlate}.</strong> При подаче нового заявления он
          может быть изменен после обработки обращения.
        </Alert>
      ) : null}

      <section className="list-card user-applications-shell">
        <div className="toolbar-stack user-applications-toolbar">
          <div className="toolbar toolbar-inline-group toolbar-row-top">
            <Form.Select
              className="toolbar-select compact-filter"
              value={sortDirection}
              onChange={(event) => setSortDirection(event.target.value)}
            >
              <option value="desc">Сначала новые</option>
              <option value="asc">Сначала старые</option>
            </Form.Select>

            <Form.Control
              className="toolbar-search compact-filter user-application-number-search"
              value={applicationNumberSearch}
              onChange={(event) => setApplicationNumberSearch(event.target.value.replace(/[^\d]/g, '').slice(0, 10))}
              placeholder="Поиск по № заявления"
            />
          </div>
        </div>

        {paginatedApplications.length ? (
          <div className="application-showcase-list application-showcase-list-spacious">
            {paginatedApplications.map((application) => (
              <article
                key={application.id}
                className="application-showcase-card application-showcase-link user-application-card user-application-card-clickable"
                role="button"
                tabIndex={0}
                onClick={() => openDetails(application.id)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    openDetails(application.id);
                  }
                }}
              >
                <div className="application-showcase-main">
                  <div className="application-showcase-head">
                    <button
                      type="button"
                      className="application-id-badge application-id-badge-button"
                      onClick={(event) => {
                        event.stopPropagation();
                        openDetails(application.id);
                      }}
                    >
                      Заявление №{application.id}
                    </button>

                    <div className="user-application-actions">
                      <span className={`status-pill ${getStatusClass(application.status)}`}>
                        {getApplicationStatusLabel(application.status)}
                      </span>
                      <div className="row-actions row-actions-nowrap">
                        <button
                          type="button"
                          className="table-action"
                          title="Открыть заявление"
                          onClick={(event) => {
                            event.stopPropagation();
                            openDetails(application.id);
                          }}
                        >
                          <i className="bi bi-eye" />
                        </button>
                        <button
                          type="button"
                          className="table-action"
                          disabled={application.status !== 'PENDING'}
                          title="Отменить заявление"
                          onClick={(event) => {
                            event.stopPropagation();
                            setCancelTarget(application);
                          }}
                        >
                          <i className="bi bi-x-circle" />
                        </button>
                      </div>
                    </div>
                  </div>

                  <div className="application-showcase-grid">
                    <div>
                      <span className="detail-label">Дата подачи</span>
                      <strong>{application.submissionDate || 'Не указана'}</strong>
                    </div>
                    <div>
                      <span className="detail-label">Номерной знак</span>
                      <strong>{application.licensePlateNumber || 'Не выбран'}</strong>
                    </div>
                    <div>
                      <span className="detail-label">Отделение</span>
                      <strong>{application.departmentName || 'Не указано'}</strong>
                    </div>
                  </div>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="empty-state">
            <i className="bi bi-journal-plus" />
            <p>У вас пока нет заявлений. Начните с создания нового.</p>
          </div>
        )}

        {sortedApplications.length > PAGE_SIZE ? (
          <div className="pagination-bar">
            <span className="helper-text">
              Показано {paginatedApplications.length} из {sortedApplications.length}
            </span>
            <Pagination className="mb-0">
              <Pagination.Prev disabled={page === 1} onClick={() => setPage((current) => Math.max(current - 1, 1))} />
              {Array.from({ length: totalPages }).map((_, index) => (
                <Pagination.Item key={index + 1} active={page === index + 1} onClick={() => setPage(index + 1)}>
                  {index + 1}
                </Pagination.Item>
              ))}
              <Pagination.Next
                disabled={page === totalPages}
                onClick={() => setPage((current) => Math.min(current + 1, totalPages))}
              />
            </Pagination>
          </div>
        ) : null}
      </section>

      <ApplicationWizardModal
        show={showWizard}
        onHide={() => setShowWizard(false)}
        onSubmit={handleCreate}
        services={services}
        departments={departments}
        passportNumber={passportNumber}
        activePlate={activePlate}
        loading={submitting}
      />

      <ApplicationDetailsModal
        show={Boolean(detailsApplication)}
        onHide={() => setDetailsApplication(null)}
        application={detailsApplication}
      />

      <ApplicationActionModal
        show={Boolean(cancelTarget)}
        onHide={() => setCancelTarget(null)}
        onConfirm={handleCancel}
        actionType="cancel"
        application={cancelTarget}
        loading={submitting}
      />
    </>
  );
}

export default UserApplications;
