import { useEffect, useMemo, useState } from 'react';
import { Button, Form, Pagination, Spinner } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { NavLink, useLocation, useSearchParams } from 'react-router-dom';
import { applicationsApi, departmentsApi, extractErrorMessage } from '../common/api';
import { getApplicationStatusLabel } from '../common/statuses';
import ConfirmModal from '../common/ConfirmModal';
import { ApplicationActionModal, ApplicationDetailsModal } from '../components/ApplicationModals';

const PAGE_SIZE = 8;

const statusOptions = [
  { value: '', label: 'Все статусы' },
  { value: 'PENDING', label: 'На рассмотрении' },
  { value: 'CONFIRMED', label: 'Подтверждено' },
  { value: 'COMPLETED', label: 'Завершено' },
  { value: 'CANCELLED', label: 'Отменено' },
  { value: 'EXPIRED', label: 'Истек срок действия' },
];

const sortOptions = [
  { value: 'desc', label: 'Сначала новые' },
  { value: 'asc', label: 'Сначала старые' },
];

const getStatusClass = (status) =>
  ({
    PENDING: 'pending',
    CONFIRMED: 'confirmed',
    COMPLETED: 'completed',
    CANCELLED: 'cancelled',
    EXPIRED: 'expired',
  }[status] || 'default');

const formatDateTime = (value) => {
  if (!value) {
    return 'Не указана';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
};

function AdminApplications() {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [applications, setApplications] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [regionFilter, setRegionFilter] = useState('');
  const [applicantSearch, setApplicantSearch] = useState('');
  const [applicationNumberSearch, setApplicationNumberSearch] = useState('');
  const [sortDirection, setSortDirection] = useState('desc');
  const [selectedIds, setSelectedIds] = useState([]);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [detailsApplication, setDetailsApplication] = useState(null);
  const [actionState, setActionState] = useState({ type: '', application: null });
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [showBulkDelete, setShowBulkDelete] = useState(false);

  const requestedApplicationId = searchParams.get('applicationId');
  const requestedDepartmentId = searchParams.get('departmentId');

  const loadApplications = async () => {
    setLoading(true);

    try {
      const [applicationsResult, departmentsResult] = await Promise.all([
        applicationsApi.getAll(),
        departmentsApi.getAll(),
      ]);

      setApplications(applicationsResult);
      setDepartments(departmentsResult);
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось загрузить заявления'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadApplications();
  }, [location.key]);

  const regionOptions = useMemo(
    () => [...new Set(departments.map((department) => department.region).filter(Boolean))],
    [departments],
  );

  const displayedApplications = useMemo(() => {
    const filtered = applications.filter((application) => {
      const statusMatches = statusFilter ? application.status === statusFilter : true;
      const departmentRegion = departments.find((department) => department.id === application.departmentId)?.region;
      const regionMatches = regionFilter
        ? application.departmentName?.toLowerCase().includes(regionFilter.toLowerCase()) || departmentRegion === regionFilter
        : true;
      const applicantMatches = applicantSearch
        ? (application.applicantName || '').toLowerCase().includes(applicantSearch.trim().toLowerCase())
        : true;
      const applicationNumberMatches = applicationNumberSearch
        ? String(application.id).includes(applicationNumberSearch.replace(/[^\d]/g, ''))
        : true;
      const departmentMatches = requestedDepartmentId
        ? String(application.departmentId || '') === requestedDepartmentId
        : true;

      return (
        statusMatches &&
        regionMatches &&
        applicantMatches &&
        applicationNumberMatches &&
        departmentMatches
      );
    });

    return filtered.sort((left, right) => {
      const leftDate = new Date(left.submissionDate || 0);
      const rightDate = new Date(right.submissionDate || 0);
      return sortDirection === 'desc' ? rightDate - leftDate : leftDate - rightDate;
    });
  }, [
    applicantSearch,
    applicationNumberSearch,
    applications,
    departments,
    regionFilter,
    requestedDepartmentId,
    sortDirection,
    statusFilter,
  ]);

  const paginatedApplications = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return displayedApplications.slice(start, start + PAGE_SIZE);
  }, [displayedApplications, page]);

  const totalPages = Math.max(Math.ceil(displayedApplications.length / PAGE_SIZE), 1);

  useEffect(() => {
    setPage(1);
  }, [
    applicantSearch,
    applicationNumberSearch,
    displayedApplications.length,
    regionFilter,
    requestedDepartmentId,
    sortDirection,
    statusFilter,
  ]);

  useEffect(() => {
    setSelectedIds((current) =>
      current.filter((id) => displayedApplications.some((application) => application.id === id)),
    );
  }, [displayedApplications]);

  useEffect(() => {
    setApplicantSearch('');
    setApplicationNumberSearch('');
    setStatusFilter('');
    setRegionFilter('');
    setSortDirection('desc');
    setSelectedIds([]);
  }, [requestedDepartmentId]);

  const openDetails = async (applicationId) => {
    try {
      const result = await applicationsApi.getWithDetails(applicationId);
      setDetailsApplication(result);
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось загрузить детали заявления'));
    }
  };

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

  const allDisplayedSelected =
    paginatedApplications.length > 0 &&
    paginatedApplications.every((application) => selectedIds.includes(application.id));

  const handleStatusAction = async (comment) => {
    if (!actionState.application || !actionState.type) {
      return;
    }

    setSubmitting(true);

    try {
      if (actionState.type === 'confirm') {
        await applicationsApi.confirm(actionState.application.id, { comment });
      }
      if (actionState.type === 'complete') {
        await applicationsApi.complete(actionState.application.id, { comment });
      }
      if (actionState.type === 'cancel') {
        await applicationsApi.cancel(actionState.application.id, { comment });
      }

      toast.success('Статус заявления обновлен');
      setActionState({ type: '', application: null });
      await loadApplications();
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось изменить статус'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) {
      return;
    }

    setSubmitting(true);

    try {
      await applicationsApi.remove(deleteTarget.id);
      toast.success('Заявление удалено');
      setDeleteTarget(null);
      setSelectedIds((current) => current.filter((id) => id !== deleteTarget.id));
      await loadApplications();
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось удалить заявление'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleBulkDelete = async () => {
    if (!selectedIds.length) {
      return;
    }

    setSubmitting(true);

    try {
      const results = await Promise.allSettled(selectedIds.map((id) => applicationsApi.remove(id)));
      const successCount = results.filter((result) => result.status === 'fulfilled').length;
      const failedCount = results.length - successCount;

      if (successCount) {
        toast.success(
          failedCount
            ? `Удалено ${successCount} заявлений, не удалось удалить ${failedCount}`
            : 'Выбранные заявления удалены',
        );
      } else {
        toast.error('Не удалось удалить выбранные заявления');
      }

      setSelectedIds([]);
      setShowBulkDelete(false);
      await loadApplications();
    } finally {
      setSubmitting(false);
    }
  };

  const toggleSelectAll = () => {
    if (allDisplayedSelected) {
      setSelectedIds((current) =>
        current.filter((id) => !paginatedApplications.some((application) => application.id === id)),
      );
      return;
    }

    setSelectedIds((current) => [
      ...new Set([...current, ...paginatedApplications.map((application) => application.id)]),
    ]);
  };

  const toggleSelectOne = (applicationId) => {
    setSelectedIds((current) =>
      current.includes(applicationId) ? current.filter((id) => id !== applicationId) : [...current, applicationId],
    );
  };

  return (
    <>
      <section className="page-header">
        <div className="page-title">
          <p className="eyebrow">Заявления</p>
          <h1>Работа с заявлениями</h1>
          <p>Статусы, поиск, фильтры и массовые действия собраны в одном аккуратном окне.</p>
        </div>
      </section>

      <section className="table-shell">
        <div className="applications-toolbar admin-applications-toolbar">
          <div className="applications-filters admin-applications-searches">
            <Form.Control
              className="toolbar-search compact-filter admin-search-wide"
              value={applicantSearch}
              onChange={(event) => setApplicantSearch(event.target.value)}
              placeholder="Поиск по ФИО"
            />
            <Form.Control
              className="toolbar-search compact-filter admin-search-wide"
              value={applicationNumberSearch}
              onChange={(event) => setApplicationNumberSearch(event.target.value.replace(/[^\d]/g, '').slice(0, 10))}
              placeholder="Поиск по № заявления"
            />
          </div>

          <div className="applications-meta admin-applications-meta">
            <Form.Select
              className="toolbar-select compact-filter"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
            >
              {statusOptions.map((status) => (
                <option key={status.value || 'all'} value={status.value}>
                  {status.label}
                </option>
              ))}
            </Form.Select>

            <Form.Select
              className="toolbar-select compact-filter"
              value={regionFilter}
              onChange={(event) => setRegionFilter(event.target.value)}
            >
              <option value="">Все регионы</option>
              {regionOptions.map((region) => (
                <option key={region} value={region}>
                  {region}
                </option>
              ))}
            </Form.Select>

            <span className="soft-badge">Найдено: {displayedApplications.length}</span>
            <span className="soft-badge">Выбрано: {selectedIds.length}</span>

            {requestedDepartmentId ? (
              <Button as={NavLink} to="/admin/applications" variant="outline-light" className="glass-button departments-return-button">
                <i className="bi bi-buildings me-2" />
                Все отделения
              </Button>
            ) : null}

            <Form.Select
              className="toolbar-select compact-filter toolbar-select-meta"
              value={sortDirection}
              onChange={(event) => setSortDirection(event.target.value)}
            >
              {sortOptions.map((sort) => (
                <option key={sort.value} value={sort.value}>
                  {sort.label}
                </option>
              ))}
            </Form.Select>

            <Button
              variant="outline-light"
              className="glass-button"
              title="Сбросить фильтры"
              onClick={() => {
                setStatusFilter('');
                setRegionFilter('');
                setApplicantSearch('');
                setApplicationNumberSearch('');
                setSortDirection('desc');
              }}
            >
              <i className="bi bi-arrow-counterclockwise me-2" />
              Сбросить
            </Button>

            <Button
              variant="outline-light"
              className="glass-button bulk-delete-button"
              disabled={!selectedIds.length}
              title="Удалить выбранные заявления"
              onClick={() => setShowBulkDelete(true)}
            >
              <i className="bi bi-trash me-2" />
              Удалить выбранные
            </Button>
          </div>
        </div>

        {loading ? (
          <div className="loading-state">
            <Spinner />
          </div>
        ) : displayedApplications.length ? (
          <>
            <div className="table-responsive">
              <table className="data-table applications-table">
                <thead>
                  <tr>
                    <th className="checkbox-col">
                      <Form.Check checked={allDisplayedSelected} onChange={toggleSelectAll} />
                    </th>
                    <th>Номер заявления</th>
                    <th>Дата</th>
                    <th>Заявитель</th>
                    <th>Знак</th>
                    <th>Отделение</th>
                    <th>Статус</th>
                    <th>Действия</th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedApplications.map((application) => (
                    <tr key={application.id}>
                      <td className="checkbox-col">
                        <Form.Check
                          checked={selectedIds.includes(application.id)}
                          onChange={() => toggleSelectOne(application.id)}
                        />
                      </td>
                      <td className="application-id-cell">
                        <button
                          type="button"
                          className="application-id-badge application-id-badge-button application-id-badge-admin"
                          onClick={() => openDetails(application.id)}
                          title={`Открыть заявление №${application.id}`}
                        >
                          Заявление №{application.id}
                        </button>
                      </td>
                      <td>{formatDateTime(application.submissionDate)}</td>
                      <td>{application.applicantName || 'Не указан'}</td>
                      <td className="plate-number-cell">{application.licensePlateNumber || 'Не назначен'}</td>
                      <td>{application.departmentName || 'Не указано'}</td>
                      <td className="status-cell">
                        <span
                          className={`status-pill status-pill-link ${getStatusClass(application.status)}`}
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
                          {getApplicationStatusLabel(application.status)}
                        </span>
                      </td>
                      <td className="actions-cell">
                        <div className="row-actions row-actions-nowrap row-actions-admin">
                          <button
                            type="button"
                            className="table-action"
                            onClick={() => openDetails(application.id)}
                            title="Открыть детали заявления"
                          >
                            <i className="bi bi-eye" />
                          </button>
                          <button
                            type="button"
                            className="table-action"
                            disabled={application.status !== 'PENDING'}
                            onClick={() => setActionState({ type: 'confirm', application })}
                            title="Подтвердить заявление"
                          >
                            <i className="bi bi-check2-circle" />
                          </button>
                          <button
                            type="button"
                            className="table-action"
                            disabled={application.status !== 'CONFIRMED'}
                            onClick={() => setActionState({ type: 'complete', application })}
                            title="Завершить заявление"
                          >
                            <i className="bi bi-flag" />
                          </button>
                          <button
                            type="button"
                            className="table-action"
                            disabled={['COMPLETED', 'CANCELLED', 'EXPIRED'].includes(application.status)}
                            onClick={() => setActionState({ type: 'cancel', application })}
                            title="Отменить заявление"
                          >
                            <i className="bi bi-x-circle" />
                          </button>
                          <button
                            type="button"
                            className="table-action"
                            onClick={() => setDeleteTarget(application)}
                            title="Удалить заявление"
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
                Показано {paginatedApplications.length} из {displayedApplications.length}
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
          </>
        ) : (
          <div className="empty-state">
            <i className="bi bi-journal-x" />
            <p>Заявления по выбранным фильтрам не найдены.</p>
          </div>
        )}
      </section>

      <ApplicationDetailsModal
        show={Boolean(detailsApplication)}
        onHide={() => setDetailsApplication(null)}
        application={detailsApplication}
      />

      <ApplicationActionModal
        show={Boolean(actionState.application)}
        onHide={() => setActionState({ type: '', application: null })}
        onConfirm={handleStatusAction}
        actionType={actionState.type}
        application={actionState.application}
        loading={submitting}
      />

      <ConfirmModal
        show={Boolean(deleteTarget)}
        onHide={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Удалить заявление"
        message="Удалить выбранное заявление?"
        confirmText="Удалить"
        loading={submitting}
      />

      <ConfirmModal
        show={showBulkDelete}
        onHide={() => setShowBulkDelete(false)}
        onConfirm={handleBulkDelete}
        title="Удалить выбранные заявления"
        message={`Удалить выбранные заявления: ${selectedIds.length}?`}
        confirmText="Удалить"
        loading={submitting}
      />
    </>
  );
}

export default AdminApplications;
