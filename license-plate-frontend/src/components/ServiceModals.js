import { useEffect, useMemo, useState } from 'react';
import { Button, Col, Form, Modal, Row } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { getApplicationStatusLabel } from '../common/statuses';

const initialForm = {
  name: '',
  description: '',
  price: '',
  isAvailable: true,
};

export function ServiceFormModal({ show, onHide, onSubmit, service, loading }) {
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState('');

  useEffect(() => {
    if (show) {
      setForm(
        service
          ? {
              name: service.name || '',
              description: service.description || '',
              price: service.price || '',
              isAvailable: service.isAvailable ?? true,
            }
          : initialForm,
      );
      setError('');
    }
  }, [service, show]);

  const validationMessage = useMemo(() => {
    if (!form.name.trim()) return 'Укажите название услуги';
    if (!Number(form.price) || Number(form.price) <= 0) return 'Цена должна быть больше нуля';
    return '';
  }, [form.name, form.price]);

  const handleChange = ({ target }) => {
    const value = target.type === 'checkbox' ? target.checked : target.value;
    setForm((current) => ({ ...current, [target.name]: value }));
    setError('');
  };

  const handleSubmit = (event) => {
    event.preventDefault();

    if (validationMessage) {
      setError(validationMessage);
      return;
    }

    onSubmit({
      ...form,
      price: Number(form.price),
    });
  };

  return (
    <Modal centered show={show} onHide={onHide} dialogClassName="glass-modal service-modal">
      <Modal.Header closeButton closeVariant="white" className="modal-dark-header">
        <Modal.Title>{service ? 'Редактирование услуги' : 'Новая услуга'}</Modal.Title>
      </Modal.Header>
      <Form onSubmit={handleSubmit}>
        <Modal.Body>
          <Row className="g-3">
            <Col md={12}>
              <Form.Group>
                <Form.Label>Название</Form.Label>
                <Form.Control name="name" value={form.name} onChange={handleChange} />
              </Form.Group>
            </Col>
            <Col md={12}>
              <Form.Group>
                <Form.Label>Описание</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={4}
                  name="description"
                  value={form.description}
                  onChange={handleChange}
                />
              </Form.Group>
            </Col>
            <Col md={6}>
              <Form.Group>
                <Form.Label>Цена</Form.Label>
                <Form.Control name="price" type="number" value={form.price} onChange={handleChange} />
              </Form.Group>
            </Col>
            <Col md={6} className="d-flex align-items-end">
              <Form.Check
                name="isAvailable"
                checked={form.isAvailable}
                onChange={handleChange}
                label="Услуга доступна"
              />
            </Col>
          </Row>
          {error ? <div className="form-error">{error}</div> : null}
        </Modal.Body>
        <Modal.Footer className="modal-dark-footer">
          <Button variant="outline-light" className="glass-button" onClick={onHide}>
            Отмена
          </Button>
          <Button type="submit" className="primary-button" disabled={loading}>
            {loading ? 'Сохраняем...' : service ? 'Сохранить' : 'Создать'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}

export function ServiceApplicationsModal({ show, onHide, service, applications = [] }) {
  return (
    <Modal centered show={show} onHide={onHide} dialogClassName="glass-modal modal-xl service-modal">
      <Modal.Header closeButton closeVariant="white" className="modal-dark-header">
        <Modal.Title>Заявления по услуге</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <div className="detail-card mb-3 detail-card-accent">
          <div className="detail-label">Услуга</div>
          <div className="detail-value">{service?.name}</div>
          {service?.description ? <div className="detail-note">{service.description}</div> : null}
        </div>

        <div className="mini-list">
          {applications.length ? (
            applications.map((application) => (
              <Link
                key={application.id}
                to={`/admin/applications?applicationId=${application.id}`}
                className="mini-list-item mini-list-link"
                onClick={onHide}
              >
                <div className="mini-list-copy">
                  <strong>Заявление №{application.id}</strong>
                  <small>
                    {application.applicantName || 'Без заявителя'} • {application.licensePlateNumber || 'Без номера'}
                  </small>
                </div>
                <span className="soft-badge">{getApplicationStatusLabel(application.status)}</span>
              </Link>
            ))
          ) : (
            <div className="empty-state compact-empty">
              <i className="bi bi-journal-x" />
              <p>Эта услуга пока не используется в заявлениях.</p>
            </div>
          )}
        </div>
      </Modal.Body>
    </Modal>
  );
}
