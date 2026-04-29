import { useEffect, useMemo, useState } from 'react';
import { Button, Form, Pagination, Spinner } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { applicationsApi, extractErrorMessage, servicesApi } from '../common/api';
import ConfirmModal from '../common/ConfirmModal';
import { ServiceApplicationsModal, ServiceFormModal } from '../components/ServiceModals';
import { getServiceStatusLabel } from '../common/statuses';

const PAGE_SIZE = 8;

function AdminServices() {
  const [services, setServices] = useState([]);
  const [applications, setApplications] = useState([]);
  const [serviceSearch, setServiceSearch] = useState('');
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [editingService, setEditingService] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [selectedService, setSelectedService] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const loadData = async () => {
    setLoading(true);

    try {
      const [servicesResult, applicationsResult] = await Promise.all([
        servicesApi.getAll(),
        applicationsApi.getAll(),
      ]);
      setServices(servicesResult);
      setApplications(applicationsResult);
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось загрузить услуги'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const filteredServices = useMemo(
    () =>
      services.filter((service) =>
        serviceSearch ? service.name?.toLowerCase().includes(serviceSearch.trim().toLowerCase()) : true,
      ),
    [serviceSearch, services],
  );

  const paginatedServices = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filteredServices.slice(start, start + PAGE_SIZE);
  }, [filteredServices, page]);

  const totalPages = Math.max(Math.ceil(filteredServices.length / PAGE_SIZE), 1);

  const relatedApplications = useMemo(() => {
    if (!selectedService) return [];
    return applications.filter((application) =>
      (application.additionalServices || []).includes(selectedService.name),
    );
  }, [applications, selectedService]);

  useEffect(() => {
    setPage(1);
  }, [filteredServices.length, serviceSearch]);

  const handleSubmit = async (payload) => {
    setSubmitting(true);

    try {
      if (editingService) {
        await servicesApi.update(editingService.id, payload);
        toast.success('Услуга обновлена');
      } else {
        await servicesApi.create(payload);
        toast.success('Услуга создана');
      }

      setShowForm(false);
      setEditingService(null);
      await loadData();
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось сохранить услугу'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;

    setSubmitting(true);

    try {
      await servicesApi.remove(deleteTarget.id);
      toast.success('Услуга удалена');
      setDeleteTarget(null);
      await loadData();
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось удалить услугу'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <section className="page-header page-header-inline">
        <div className="page-title">
          <p className="eyebrow">Услуги</p>
          <h1>Каталог дополнительных услуг</h1>
          <p>Быстро ищите нужную услугу, проверяйте связанные заявления и управляйте доступностью без лишних переходов.</p>
        </div>
        <div className="page-actions">
          <Button className="primary-button" onClick={() => setShowForm(true)}>
            <i className="bi bi-plus-circle me-2" />
            Добавить услугу
          </Button>
        </div>
      </section>

      <section className="table-shell">
        <div className="toolbar toolbar-compact toolbar-filters-single-row">
          <Form.Control
            className="toolbar-search compact-filter"
            value={serviceSearch}
            onChange={(event) => setServiceSearch(event.target.value)}
            placeholder="Поиск по названию услуги"
          />
        </div>

        {loading ? (
          <div className="loading-state">
            <Spinner />
          </div>
        ) : filteredServices.length ? (
          <>
            <div className="table-responsive">
              <table className="data-table services-table">
                <thead>
                  <tr>
                    <th>Название</th>
                    <th>Описание</th>
                    <th>Цена</th>
                    <th>Доступность</th>
                    <th>Действия</th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedServices.map((service) => (
                    <tr key={service.id}>
                      <td>{service.name}</td>
                      <td>{service.description || 'Без описания'}</td>
                      <td>{service.price} BYN</td>
                      <td>
                        <span className={`status-pill ${service.isAvailable ? 'available' : 'cancelled'}`}>
                          {getServiceStatusLabel(service.isAvailable ? 'AVAILABLE' : 'DISABLED')}
                        </span>
                      </td>
                      <td>
                        <div className="row-actions row-actions-nowrap">
                          <button type="button" className="table-action" onClick={() => setSelectedService(service)}>
                            <i className="bi bi-journal-richtext" />
                          </button>
                          <button
                            type="button"
                            className="table-action"
                            onClick={() => {
                              setEditingService(service);
                              setShowForm(true);
                            }}
                          >
                            <i className="bi bi-pencil-square" />
                          </button>
                          <button type="button" className="table-action" onClick={() => setDeleteTarget(service)}>
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
                Показано {paginatedServices.length} из {filteredServices.length}
              </span>
              <Pagination className="mb-0">
                <Pagination.Prev disabled={page === 1} onClick={() => setPage((current) => Math.max(current - 1, 1))} />
                {Array.from({ length: totalPages }).map((_, index) => (
                  <Pagination.Item key={index + 1} active={page === index + 1} onClick={() => setPage(index + 1)}>
                    {index + 1}
                  </Pagination.Item>
                ))}
                <Pagination.Next disabled={page === totalPages} onClick={() => setPage((current) => Math.min(current + 1, totalPages))} />
              </Pagination>
            </div>
          </>
        ) : (
          <div className="empty-state">
            <i className="bi bi-stars" />
            <p>Услуги не найдены.</p>
          </div>
        )}
      </section>

      <ServiceFormModal
        show={showForm}
        onHide={() => {
          setShowForm(false);
          setEditingService(null);
        }}
        onSubmit={handleSubmit}
        service={editingService}
        loading={submitting}
      />

      <ServiceApplicationsModal
        show={Boolean(selectedService)}
        onHide={() => setSelectedService(null)}
        service={selectedService}
        applications={relatedApplications}
      />

      <ConfirmModal
        show={Boolean(deleteTarget)}
        onHide={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Удалить услугу"
        message={`Удалить услугу "${deleteTarget?.name}"?`}
        confirmText="Удалить"
        loading={submitting}
      />
    </>
  );
}

export default AdminServices;
