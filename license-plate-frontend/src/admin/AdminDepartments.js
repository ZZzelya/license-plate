import { useEffect, useMemo, useState } from 'react';
import { Button, Form, Pagination, Spinner } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { NavLink, useLocation } from 'react-router-dom';
import { departmentsApi, extractErrorMessage } from '../common/api';
import ConfirmModal from '../common/ConfirmModal';
import { DepartmentFormModal } from '../components/DepartmentModals';

const PAGE_SIZE = 8;

function AdminDepartments() {
  const location = useLocation();
  const [departments, setDepartments] = useState([]);
  const [regionFilter, setRegionFilter] = useState('');
  const [nameSearch, setNameSearch] = useState('');
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [editingDepartment, setEditingDepartment] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const loadDepartments = async () => {
    setLoading(true);

    try {
      const result = regionFilter ? await departmentsApi.getByRegion(regionFilter) : await departmentsApi.getAll();
      setDepartments(result);
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось загрузить отделения'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDepartments();
  }, [location.key, regionFilter]);

  const regionOptions = useMemo(
    () => [...new Set(departments.map((department) => department.region).filter(Boolean))],
    [departments],
  );

  const filteredDepartments = useMemo(
    () =>
      departments.filter((department) =>
        nameSearch ? department.name?.toLowerCase().includes(nameSearch.trim().toLowerCase()) : true,
      ),
    [departments, nameSearch],
  );

  const paginatedDepartments = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filteredDepartments.slice(start, start + PAGE_SIZE);
  }, [filteredDepartments, page]);

  const totalPages = Math.max(Math.ceil(filteredDepartments.length / PAGE_SIZE), 1);

  useEffect(() => {
    setPage(1);
  }, [nameSearch, regionFilter, filteredDepartments.length]);

  const handleSubmit = async (payload) => {
    setSubmitting(true);

    try {
      if (editingDepartment) {
        await departmentsApi.update(editingDepartment.id, payload);
        toast.success('Отделение обновлено');
      } else {
        await departmentsApi.create(payload);
        toast.success('Отделение создано');
      }

      setShowForm(false);
      setEditingDepartment(null);
      await loadDepartments();
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось сохранить отделение'));
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
      await departmentsApi.remove(deleteTarget.id);
      toast.success('Отделение удалено');
      setDeleteTarget(null);
      await loadDepartments();
    } catch (error) {
      toast.error(extractErrorMessage(error, 'Не удалось удалить отделение'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <section className="page-header page-header-inline">
        <div className="page-title">
          <p className="eyebrow">Отделения</p>
          <h1>Управление отделениями</h1>
          <p>Фильтруйте отделения по региону, быстро открывайте связанные заявления и управляйте данными в одном месте.</p>
        </div>
        <div className="page-actions">
          <Button className="primary-button" onClick={() => setShowForm(true)}>
            <i className="bi bi-building-add me-2" />
            Добавить отделение
          </Button>
        </div>
      </section>

      <section className="table-shell">
        <div className="toolbar toolbar-filters-single-row departments-toolbar-row">
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

          <Form.Control
            className="toolbar-search compact-filter"
            value={nameSearch}
            onChange={(event) => setNameSearch(event.target.value)}
            placeholder="Поиск по названию отделения"
          />
        </div>

        {loading ? (
          <div className="loading-state">
            <Spinner />
          </div>
        ) : filteredDepartments.length ? (
          <>
            <div className="table-responsive">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Название</th>
                    <th>Регион</th>
                    <th>Телефон</th>
                    <th>Адрес</th>
                    <th>Заявления</th>
                    <th>Действия</th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedDepartments.map((department) => (
                    <tr key={department.id}>
                      <td>{department.name}</td>
                      <td>{department.region}</td>
                      <td>{department.phoneNumber || 'Не указан'}</td>
                      <td>{department.address || 'Не указан'}</td>
                      <td>
                        <NavLink to={`/admin/applications?departmentId=${department.id}`} className="soft-badge soft-badge-link">
                          {department.applicationsCount ?? 0}
                        </NavLink>
                      </td>
                      <td>
                        <div className="row-actions row-actions-nowrap">
                          <NavLink
                            to={`/admin/applications?departmentId=${department.id}`}
                            className="table-action"
                            title="Перейти к заявлениям отделения"
                          >
                            <i className="bi bi-journal-text" />
                          </NavLink>
                          <button
                            type="button"
                            className="table-action"
                            onClick={() => {
                              setEditingDepartment(department);
                              setShowForm(true);
                            }}
                          >
                            <i className="bi bi-pencil-square" />
                          </button>
                          <button type="button" className="table-action" onClick={() => setDeleteTarget(department)}>
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
                Показано {paginatedDepartments.length} из {filteredDepartments.length}
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
            <i className="bi bi-buildings" />
            <p>Отделения не найдены.</p>
          </div>
        )}
      </section>

      <DepartmentFormModal
        show={showForm}
        onHide={() => {
          setShowForm(false);
          setEditingDepartment(null);
        }}
        onSubmit={handleSubmit}
        department={editingDepartment}
        loading={submitting}
      />

      <ConfirmModal
        show={Boolean(deleteTarget)}
        onHide={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Удалить отделение"
        message={`Удалить отделение "${deleteTarget?.name}"?`}
        confirmText="Удалить"
        loading={submitting}
      />
    </>
  );
}

export default AdminDepartments;
