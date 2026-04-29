import { useEffect, useMemo, useState } from 'react';
import { Button, Col, Form, Modal, Row } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { formatBelarusPhone } from '../common/formatters';
import { getApplicationStatusLabel } from '../common/statuses';

const initialForm = {
  fullName: '',
  passportNumber: '',
  phoneNumber: '',
  email: '',
  address: '',
};

const getApplicationStatusClass = (status) =>
  ({
    PENDING: 'pending',
    CONFIRMED: 'confirmed',
    COMPLETED: 'completed',
    CANCELLED: 'cancelled',
    REJECTED: 'cancelled',
    EXPIRED: 'expired',
  }[status] || 'default');

export function ApplicantFormModal({ show, onHide, onSubmit, applicant, loading }) {
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState('');

  useEffect(() => {
    if (show) {
      setForm(
        applicant
          ? {
              fullName: applicant.fullName || '',
              passportNumber: applicant.passportNumber || '',
              phoneNumber: applicant.phoneNumber || '',
              email: applicant.email || '',
              address: applicant.address || '',
            }
          : initialForm,
      );
      setError('');
    }
  }, [applicant, show]);

  const validationMessage = useMemo(() => {
    if (!form.fullName.trim()) return 'Укажите ФИО';
    if (!/^[A-Z]{2}\d{7}$/.test(form.passportNumber.trim().toUpperCase())) {
      return 'Паспорт должен быть в формате MP1234567';
    }
    return '';
  }, [form.fullName, form.passportNumber]);

  const handleChange = ({ target }) => {
    let value = target.value;

    if (target.name === 'passportNumber') {
      value = value.toUpperCase();
    }

    if (target.name === 'phoneNumber') {
      value = formatBelarusPhone(value);
    }

    setForm((current) => ({ ...current, [target.name]: value }));
    setError('');
  };

  const handleSubmit = (event) => {
    event.preventDefault();

    if (validationMessage) {
      setError(validationMessage);
      return;
    }

    onSubmit(form);
  };

  return (
    <Modal centered show={show} onHide={onHide} dialogClassName="glass-modal">
      <Modal.Header closeButton closeVariant="white" className="modal-dark-header">
        <Modal.Title>{applicant ? 'Редактирование заявителя' : 'Новый заявитель'}</Modal.Title>
      </Modal.Header>

      <Form onSubmit={handleSubmit}>
        <Modal.Body>
          <Row className="g-3">
            <Col md={12}>
              <Form.Group>
                <Form.Label>ФИО</Form.Label>
                <Form.Control name="fullName" value={form.fullName} onChange={handleChange} />
              </Form.Group>
            </Col>

            <Col md={6}>
              <Form.Group>
                <Form.Label>Паспорт</Form.Label>
                <Form.Control
                  name="passportNumber"
                  value={form.passportNumber}
                  onChange={handleChange}
                  placeholder="MP1234567"
                />
              </Form.Group>
            </Col>

            <Col md={6}>
              <Form.Group>
                <Form.Label>Телефон</Form.Label>
                <Form.Control
                  name="phoneNumber"
                  value={form.phoneNumber}
                  onChange={handleChange}
                  placeholder="+375 (29) 367-45-67"
                />
              </Form.Group>
            </Col>

            <Col md={12}>
              <Form.Group>
                <Form.Label>Email</Form.Label>
                <Form.Control name="email" type="email" value={form.email} onChange={handleChange} />
              </Form.Group>
            </Col>

            <Col md={12}>
              <Form.Group>
                <Form.Label>Адрес</Form.Label>
                <Form.Control name="address" value={form.address} onChange={handleChange} />
              </Form.Group>
            </Col>
          </Row>

          {error ? <div className="form-error">{error}</div> : null}
        </Modal.Body>

        <Modal.Footer className="modal-dark-footer">
          <Button variant="outline-light" className="glass-button" onClick={onHide}>
            Отмена
          </Button>
          <Button type="submit" className="primary-button" disabled={loading}>
            {loading ? 'Сохраняем...' : applicant ? 'Сохранить' : 'Создать'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}

export function ApplicantViewModal({ show, onHide, applicant, applications = [], onToggleAdmin, loading }) {
  return (
    <Modal centered show={show} onHide={onHide} dialogClassName="glass-modal applicant-view-modal">
      <Modal.Header closeButton closeVariant="white" className="modal-dark-header">
        <Modal.Title>Карточка заявителя</Modal.Title>
      </Modal.Header>

      <Modal.Body>
        {applicant ? (
          <>
            <div className="detail-grid applicant-detail-grid">
              <div className="detail-card">
                <div className="detail-label">ФИО</div>
                <div className="detail-value">{applicant.fullName}</div>
              </div>
              <div className="detail-card">
                <div className="detail-label">Паспорт</div>
                <div className="detail-value">{applicant.passportNumber}</div>
              </div>
              <div className="detail-card">
                <div className="detail-label">Телефон</div>
                <div className="detail-value">{applicant.phoneNumber || 'Не указан'}</div>
              </div>
              <div className="detail-card applicant-detail-wide">
                <div className="detail-label">Email</div>
                <div className="detail-value applicant-email-value">{applicant.email || 'Не указан'}</div>
              </div>
              <div className="detail-card">
                <div className="detail-label">Адрес</div>
                <div className="detail-value">{applicant.address || 'Не указан'}</div>
              </div>
              <div className="detail-card">
                <div className="detail-label">Количество заявлений</div>
                <div className="detail-value">{applicant.applicationsCount ?? 0}</div>
              </div>
              <div className="detail-card">
                <div className="detail-label">Роль</div>
                <div className="detail-value">{applicant.role === 'ADMIN' ? 'Администратор' : 'Пользователь'}</div>
              </div>
              <div className="detail-card applicant-detail-wide applicant-detail-actions">
                <div className="detail-label">Управление ролью</div>
                <Button
                  type="button"
                  className={applicant.role === 'ADMIN' ? 'glass-button' : 'primary-button'}
                  disabled={!applicant.hasUserAccount || loading}
                  onClick={() => onToggleAdmin?.(applicant)}
                >
                  <i className={`bi ${applicant.role === 'ADMIN' ? 'bi-person-dash' : 'bi-person-gear'} me-2`} />
                  {applicant.role === 'ADMIN' ? 'Сделать пользователем' : 'Сделать администратором'}
                </Button>
              </div>
            </div>

            <div className="applicant-applications-panel">
              <div className="section-title-row">
                <h3 className="section-title">Все заявления заявителя</h3>
                <span className="soft-badge">{applications.length}</span>
              </div>

              {applications.length ? (
                <div className="applicant-application-list">
                  {applications.map((application) => (
                    <Link
                      key={application.id}
                      to={`/admin/applications?applicationId=${application.id}`}
                      className="applicant-application-link"
                      onClick={onHide}
                    >
                      <div>
                        <strong>Заявление №{application.id}</strong>
                        <span>{application.licensePlateNumber || 'Номер не назначен'}</span>
                      </div>
                      <span className={`status-pill ${getApplicationStatusClass(application.status)}`}>
                        {getApplicationStatusLabel(application.status)}
                      </span>
                    </Link>
                  ))}
                </div>
              ) : (
                <div className="empty-state compact-empty">
                  <i className="bi bi-journal-text" />
                  <p>У заявителя пока нет заявлений.</p>
                </div>
              )}
            </div>
          </>
        ) : null}
      </Modal.Body>
    </Modal>
  );
}
