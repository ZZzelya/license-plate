import { useEffect, useMemo, useState } from 'react';
import { Button, Form, Pagination, Spinner } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { useLocation, useSearchParams } from 'react-router-dom';
import {
  applicantsApi,
  applicationsApi,
  extractErrorMessage,
  passportChangeRequestsApi,
} from '../common/api';
import ConfirmModal from '../common/ConfirmModal';
import { ApplicantFormModal, ApplicantViewModal } from '../components/ApplicantModals';

const PAGE_SIZE = 8;

const REQUEST_STATUS_LABELS = {
  PENDING: 'На рассмотрении',
  APPROVED: 'Подтверждена',
  REJECTED: 'Отклонена',
};

function AdminApplicants() {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [applicants, setApplicants] = useState([]);
  const [applications, setApplications] = useState([]);
  const [passportRequests, setPassportRequests] = useState([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [requestsLoading, setRequestsLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [selectedApplicant, setSelectedApplicant] = useState(null);
  const [editingApplicant, setEditingApplicant] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [showView, setShowView] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const requestedApplicantId = searchParams.get('applicantId');

  const loadApplicants = async () => {
    setLoading(true);

    try {
      const result = await applicantsApi.getAll();
      setApplicants(result);
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось загрузить заявителей'));
    } finally {
      setLoading(false);
    }
  };

  const loadPassportRequests = async () => {
    setRequestsLoading(true);

    try {
      const result = await passportChangeRequestsApi.getAll();
      setPassportRequests(result);
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось загрузить заявки на смену паспорта'));
    } finally {
      setRequestsLoading(false);
    }
  };

  const loadApplications = async () => {
    try {
      const result = await applicationsApi.getAll();
      setApplications(result);
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось загрузить заявления заявителей'));
    }
  };

  useEffect(() => {
    loadApplicants();
    loadPassportRequests();
    loadApplications();
  }, [location.key]);

  useEffect(() => {
    const timer = setTimeout(async () => {
      if (!search.trim()) {
        loadApplicants();
        return;
      }

      try {
        setLoading(true);
        const result = await applicantsApi.getByPassport(search.trim().toUpperCase());
        setApplicants(result ? [result] : []);
        setPage(1);
      } catch {
        setApplicants([]);
      } finally {
        setLoading(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    if (!requestedApplicantId || loading) {
      return;
    }

    const targetId = Number(requestedApplicantId);
    if (!Number.isFinite(targetId)) {
      return;
    }

    const applicant = applicants.find((item) => item.id === targetId);
    if (!applicant) {
      return;
    }

    setSelectedApplicant(applicant);
    setShowView(true);

    const nextParams = new URLSearchParams(searchParams);
    nextParams.delete('applicantId');
    setSearchParams(nextParams, { replace: true });
  }, [applicants, loading, requestedApplicantId]);

  const pendingPassportRequests = useMemo(
    () => passportRequests.filter((request) => request.status === 'PENDING'),
    [passportRequests],
  );

  const paginatedApplicants = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return applicants.slice(start, start + PAGE_SIZE);
  }, [applicants, page]);

  const totalPages = Math.max(Math.ceil(applicants.length / PAGE_SIZE), 1);

  const selectedApplicantApplications = useMemo(() => {
    if (!selectedApplicant) {
      return [];
    }

    return applications
      .filter((application) => application.applicantPassport === selectedApplicant.passportNumber)
      .sort((left, right) => new Date(right.submissionDate) - new Date(left.submissionDate));
  }, [applications, selectedApplicant]);

  const handleSubmit = async (payload) => {
    setSubmitting(true);

    try {
      if (editingApplicant) {
        await applicantsApi.update(editingApplicant.id, payload);
        toast.success('Данные заявителя обновлены');
      } else {
        await applicantsApi.create(payload);
        toast.success('Заявитель создан');
      }

      setShowForm(false);
      setEditingApplicant(null);
      await loadApplicants();
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось сохранить заявителя'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;

    setSubmitting(true);

    try {
      await applicantsApi.remove(deleteTarget.id);
      toast.success('Заявитель удален');
      setDeleteTarget(null);
      await loadApplicants();
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось удалить заявителя'));
    } finally {
      setSubmitting(false);
    }
  };

  const handlePassportRequestAction = async (requestId, action) => {
    setSubmitting(true);

    try {
      if (action === 'approve') {
        await passportChangeRequestsApi.approve(requestId);
        toast.success('Заявка на смену паспорта подтверждена');
      } else {
        await passportChangeRequestsApi.reject(requestId);
        toast.success('Заявка на смену паспорта отклонена');
      }

      await Promise.all([loadApplicants(), loadPassportRequests()]);
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось обработать заявку'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <section className="page-header page-header-inline">
        <div className="page-title">
          <p className="eyebrow">Заявители</p>
          <h1>Работа с заявителями</h1>
          <p>Поиск по паспорту, просмотр карточек и обработка заявок на смену паспорта в одном разделе.</p>
        </div>
        <div className="page-actions">
          <Button className="primary-button" onClick={() => setShowForm(true)}>
            <i className="bi bi-plus-circle me-2" />
            Добавить заявителя
          </Button>
        </div>
      </section>

      <section className="list-card">
        <div className="section-title-row">
          <div>
            <h2 className="section-title">Заявки на смену паспорта</h2>
            <p className="helper-text mb-0">
              Здесь администратор подтверждает или отклоняет запросы пользователей.
            </p>
          </div>
          <span className="soft-badge">Ожидают: {pendingPassportRequests.length}</span>
        </div>

        {requestsLoading ? (
          <div className="loading-state compact-loading">
            <Spinner />
          </div>
        ) : pendingPassportRequests.length ? (
          <div className="request-list">
            {pendingPassportRequests.map((request) => (
              <article key={request.id} className="request-card">
                <div className="request-main">
                  <strong>{request.applicantName}</strong>
                  <span className="helper-text">
                    {request.currentPassportNumber} → {request.requestedPassportNumber}
                  </span>
                  <span className="helper-text">Телефон для связи: {request.contactPhone}</span>
                </div>

                <div className="request-actions">
                  <span className="soft-badge">{REQUEST_STATUS_LABELS[request.status] || request.status}</span>
                  <Button
                    size="sm"
                    className="primary-button"
                    disabled={submitting}
                    onClick={() => handlePassportRequestAction(request.id, 'approve')}
                  >
                    Подтвердить
                  </Button>
                  <Button
                    size="sm"
                    className="glass-button"
                    disabled={submitting}
                    onClick={() => handlePassportRequestAction(request.id, 'reject')}
                  >
                    Отклонить
                  </Button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="empty-state compact-empty">
            <i className="bi bi-person-vcard" />
            <p>Новых заявок на смену паспорта сейчас нет.</p>
          </div>
        )}
      </section>

      <section className="table-shell">
        <div className="toolbar toolbar-compact">
          <Form.Control
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Поиск по паспорту"
            className="toolbar-search"
          />
        </div>

        {loading ? (
          <div className="loading-state">
            <Spinner />
          </div>
        ) : applicants.length ? (
          <>
            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ФИО</th>
                    <th>Паспорт</th>
                    <th>Телефон</th>
                    <th>Email</th>
                    <th>Заявления</th>
                    <th>Действия</th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedApplicants.map((applicant) => (
                    <tr key={applicant.id}>
                      <td>{applicant.fullName}</td>
                      <td>{applicant.passportNumber}</td>
                      <td>{applicant.phoneNumber || 'Не указан'}</td>
                      <td>{applicant.email || 'Не указан'}</td>
                      <td>{applicant.applicationsCount ?? 0}</td>
                      <td>
                        <div className="row-actions">
                          <button
                            type="button"
                            className="table-action"
                            onClick={() => {
                              setSelectedApplicant(applicant);
                              setShowView(true);
                            }}
                          >
                            <i className="bi bi-eye" />
                          </button>
                          <button
                            type="button"
                            className="table-action"
                            onClick={() => {
                              setEditingApplicant(applicant);
                              setShowForm(true);
                            }}
                          >
                            <i className="bi bi-pencil-square" />
                          </button>
                          <button
                            type="button"
                            className="table-action"
                            onClick={() => setDeleteTarget(applicant)}
                          >
                            <i className="bi bi-trash" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="pagination-bar">
              <span className="helper-text">
                Показано {paginatedApplicants.length} из {applicants.length}
              </span>
              <Pagination className="mb-0">
                <Pagination.Prev
                  disabled={page === 1}
                  onClick={() => setPage((current) => Math.max(current - 1, 1))}
                />
                {Array.from({ length: totalPages }).map((_, index) => (
                  <Pagination.Item
                    key={index + 1}
                    active={page === index + 1}
                    onClick={() => setPage(index + 1)}
                  >
                    {index + 1}
                  </Pagination.Item>
                ))}
                <Pagination.Next
                  disabled={page === totalPages}
                  onClick={() => setPage((current) => Math.min(current + 1, totalPages))}
                />
              </Pagination>
            </div>
          </>
        ) : (
          <div className="empty-state">
            <i className="bi bi-people" />
            <p>Заявители не найдены.</p>
          </div>
        )}
      </section>

      <ApplicantFormModal
        show={showForm}
        onHide={() => {
          setShowForm(false);
          setEditingApplicant(null);
        }}
        onSubmit={handleSubmit}
        applicant={editingApplicant}
        loading={submitting}
      />

      <ApplicantViewModal
        show={showView}
        onHide={() => {
          setShowView(false);
          setSelectedApplicant(null);
        }}
        applicant={selectedApplicant}
        applications={selectedApplicantApplications}
      />

      <ConfirmModal
        show={Boolean(deleteTarget)}
        onHide={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Удалить заявителя"
        message={`Удалить "${deleteTarget?.fullName}" из системы?`}
        confirmText="Удалить"
        loading={submitting}
      />
    </>
  );
}

export default AdminApplicants;
