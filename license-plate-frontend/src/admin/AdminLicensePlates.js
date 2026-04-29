import { useEffect, useMemo, useState } from 'react';
import { Button, Form, Pagination, Spinner } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { departmentsApi, extractErrorMessage, licensePlatesApi } from '../common/api';
import ConfirmModal from '../common/ConfirmModal';
import { getPlateStatusLabel } from '../common/statuses';
import { LicensePlateFormModal, LicensePlateViewModal } from '../components/LicensePlateModals';

const PAGE_SIZE = 8;

const inferPlateStatus = (plate, availableNumbers) => {
  if (availableNumbers.has(plate.plateNumber)) return 'AVAILABLE';
  if (plate.issueDate) return 'ISSUED';
  return 'RESERVED';
};

const getStatusClass = (status) =>
  ({
    AVAILABLE: 'available',
    RESERVED: 'reserved',
    ISSUED: 'issued',
  }[status] || 'default');

const normalizePlateSearch = (value = '') => value.toUpperCase().replace(/\s+/g, '');

function AdminLicensePlates() {
  const [plates, setPlates] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('');
  const [plateSearch, setPlateSearch] = useState('');
  const [page, setPage] = useState(1);
  const [selectedIds, setSelectedIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [editingPlate, setEditingPlate] = useState(null);
  const [viewPlate, setViewPlate] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [showBulkDelete, setShowBulkDelete] = useState(false);

  const loadData = async () => {
    setLoading(true);

    try {
      const departmentsResult = await departmentsApi.getAll();
      const platesResult = await licensePlatesApi.getAll();
      const availableResult = await Promise.all(
        departmentsResult.map((department) =>
          licensePlatesApi.getAvailableByDepartment(department.id).catch(() => []),
        ),
      );
      const availableNumbers = new Set(availableResult.flat().map((plate) => plate.plateNumber));
      const withStatus = platesResult.map((plate) => ({
        ...plate,
        derivedStatus: inferPlateStatus(plate, availableNumbers),
      }));

      setDepartments(departmentsResult);
      setPlates(withStatus);
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось загрузить номерные знаки'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const filteredPlates = useMemo(
    () =>
      plates.filter((plate) => {
        const matchesStatus = statusFilter ? plate.derivedStatus === statusFilter : true;
        const matchesDepartment = departmentFilter ? plate.departmentId === Number(departmentFilter) : true;
        const matchesSearch = plateSearch
          ? normalizePlateSearch(plate.plateNumber).includes(normalizePlateSearch(plateSearch))
          : true;

        return matchesStatus && matchesDepartment && matchesSearch;
      }),
    [departmentFilter, plateSearch, plates, statusFilter],
  );

  const paginatedPlates = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filteredPlates.slice(start, start + PAGE_SIZE);
  }, [filteredPlates, page]);

  const totalPages = Math.max(Math.ceil(filteredPlates.length / PAGE_SIZE), 1);

  useEffect(() => {
    setPage(1);
  }, [departmentFilter, plateSearch, statusFilter]);

  useEffect(() => {
    setSelectedIds((current) => current.filter((id) => filteredPlates.some((plate) => plate.id === id)));
  }, [filteredPlates]);

  const allDisplayedSelected =
    paginatedPlates.length > 0 && paginatedPlates.every((plate) => selectedIds.includes(plate.id));

  const handleSubmit = async (payload) => {
    setSubmitting(true);

    try {
      if (editingPlate) {
        await licensePlatesApi.update(editingPlate.id, payload);
        toast.success('Номерной знак обновлен');
      } else {
        await licensePlatesApi.create(payload);
        toast.success('Номерной знак создан');
      }

      setShowForm(false);
      setEditingPlate(null);
      await loadData();
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось сохранить номерной знак'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;

    setSubmitting(true);

    try {
      await licensePlatesApi.remove(deleteTarget.id);
      toast.success('Номерной знак удален');
      setDeleteTarget(null);
      setSelectedIds((current) => current.filter((id) => id !== deleteTarget.id));
      await loadData();
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось удалить номерной знак'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleBulkDelete = async () => {
    if (!selectedIds.length) return;

    setSubmitting(true);

    try {
      const results = await Promise.allSettled(selectedIds.map((id) => licensePlatesApi.remove(id)));
      const successCount = results.filter((result) => result.status === 'fulfilled').length;
      const failedCount = results.length - successCount;

      if (successCount) {
        toast.success(
          failedCount
            ? `Удалено ${successCount} знаков, не удалось удалить ${failedCount}`
            : 'Выбранные номерные знаки удалены',
        );
      } else {
        toast.error('Не удалось удалить выбранные номерные знаки');
      }

      setSelectedIds([]);
      setShowBulkDelete(false);
      await loadData();
    } finally {
      setSubmitting(false);
    }
  };

  const toggleSelectAll = () => {
    if (allDisplayedSelected) {
      setSelectedIds((current) => current.filter((id) => !paginatedPlates.some((plate) => plate.id === id)));
      return;
    }

    setSelectedIds((current) => [...new Set([...current, ...paginatedPlates.map((plate) => plate.id)])]);
  };

  const toggleSelectOne = (plateId) => {
    setSelectedIds((current) =>
      current.includes(plateId) ? current.filter((id) => id !== plateId) : [...current, plateId],
    );
  };

  return (
    <>
      <section className="page-header page-header-inline">
        <div className="page-title">
          <p className="eyebrow">Номерные знаки</p>
          <h1>Каталог номерных знаков</h1>
          <p>Ищите по номеру, фильтруйте по статусу и отделению, а также выполняйте массовые действия в одном окне.</p>
        </div>
        <div className="page-actions">
          <Button className="primary-button" onClick={() => setShowForm(true)}>
            <i className="bi bi-plus-circle me-2" />
            Добавить знак
          </Button>
        </div>
      </section>

      <section className="table-shell">
        <div className="toolbar toolbar-filters-single-row admin-plates-inline-toolbar">
            <Form.Select className="toolbar-select compact-filter" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              <option value="">Все статусы</option>
              <option value="AVAILABLE">Доступен</option>
              <option value="RESERVED">Зарезервирован</option>
              <option value="ISSUED">Выдан</option>
            </Form.Select>

            <Form.Select
              className="toolbar-select compact-filter"
              value={departmentFilter}
              onChange={(event) => setDepartmentFilter(event.target.value)}
            >
              <option value="">Все отделения</option>
              {departments.map((department) => (
                <option key={department.id} value={department.id}>
                  {department.name}
                </option>
              ))}
            </Form.Select>

            <Form.Control
              className="toolbar-search compact-filter"
              value={plateSearch}
              onChange={(event) => setPlateSearch(event.target.value.toUpperCase())}
              placeholder="Поиск по номеру: 3256 AB-7 или E001 AA-7"
            />
          
            <span className="soft-badge">Найдено: {filteredPlates.length}</span>
            <span className="soft-badge">Выбрано: {selectedIds.length}</span>
            <Button
              variant="outline-light"
              className="glass-button"
              title="Сбросить фильтры"
              onClick={() => {
                setStatusFilter('');
                setDepartmentFilter('');
                setPlateSearch('');
              }}
            >
              <i className="bi bi-arrow-counterclockwise me-2" />
              Сбросить
            </Button>
            <Button
              variant="outline-light"
              className="glass-button bulk-delete-button"
              disabled={!selectedIds.length}
              title="Удалить выбранные номерные знаки"
              onClick={() => setShowBulkDelete(true)}
            >
              <i className="bi bi-trash me-2" />
              Удалить выбранные
            </Button>
        </div>

        {loading ? (
          <div className="loading-state">
            <Spinner />
          </div>
        ) : filteredPlates.length ? (
          <>
            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th className="checkbox-col">
                      <Form.Check checked={allDisplayedSelected} onChange={toggleSelectAll} />
                    </th>
                    <th>Полный номер</th>
                    <th>Номер</th>
                    <th>Серия</th>
                    <th>Регион</th>
                    <th>Отделение</th>
                    <th>Статус</th>
                    <th>Действия</th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedPlates.map((plate) => (
                    <tr key={plate.id}>
                      <td className="checkbox-col">
                        <Form.Check checked={selectedIds.includes(plate.id)} onChange={() => toggleSelectOne(plate.id)} />
                      </td>
                      <td>{plate.plateNumber}</td>
                      <td>{plate.numberPart || 'Не указан'}</td>
                      <td>{plate.series || 'Не указана'}</td>
                      <td>{plate.region || plate.regionCode || 'Не указан'}</td>
                      <td>{plate.departmentName || 'Не указано'}</td>
                      <td>
                        <span className={`status-pill ${getStatusClass(plate.derivedStatus)}`}>
                          {getPlateStatusLabel(plate.derivedStatus)}
                        </span>
                      </td>
                      <td>
                        <div className="row-actions">
                          <button
                            type="button"
                            className="table-action"
                            onClick={() => setViewPlate(plate)}
                            title="Просмотреть номерной знак"
                          >
                            <i className="bi bi-eye" />
                          </button>
                          <button
                            type="button"
                            className="table-action"
                            title="Редактировать номерной знак"
                            onClick={() => {
                              setEditingPlate(plate);
                              setShowForm(true);
                            }}
                          >
                            <i className="bi bi-pencil-square" />
                          </button>
                          <button
                            type="button"
                            className="table-action"
                            title="Удалить номерной знак"
                            onClick={() => setDeleteTarget(plate)}
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
                Показано {paginatedPlates.length} из {filteredPlates.length}
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
            <i className="bi bi-credit-card-2-front" />
            <p>Номерные знаки не найдены.</p>
          </div>
        )}
      </section>

      <LicensePlateFormModal
        show={showForm}
        onHide={() => {
          setShowForm(false);
          setEditingPlate(null);
        }}
        onSubmit={handleSubmit}
        plate={editingPlate}
        departments={departments}
        loading={submitting}
      />

      <LicensePlateViewModal show={Boolean(viewPlate)} onHide={() => setViewPlate(null)} plate={viewPlate} />

      <ConfirmModal
        show={Boolean(deleteTarget)}
        onHide={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Удалить номерной знак"
        message={`Удалить знак ${deleteTarget?.plateNumber}?`}
        confirmText="Удалить"
        loading={submitting}
      />

      <ConfirmModal
        show={showBulkDelete}
        onHide={() => setShowBulkDelete(false)}
        onConfirm={handleBulkDelete}
        title="Удалить выбранные номерные знаки"
        message={`Удалить выбранные номерные знаки: ${selectedIds.length}?`}
        confirmText="Удалить"
        loading={submitting}
      />
    </>
  );
}

export default AdminLicensePlates;
