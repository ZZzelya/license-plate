import { useEffect, useMemo, useState } from 'react';
import { Button, Col, Form, Modal, Row } from 'react-bootstrap';
import { formatBelarusPhone } from '../common/formatters';

const initialForm = {
  name: '',
  address: '',
  phoneNumber: '',
  region: '',
};

const PHONE_PATTERN = /^\+375 \((25|29|33|44)\) \d{3}-\d{2}-\d{2}$/;

export function DepartmentFormModal({ show, onHide, onSubmit, department, loading }) {
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState('');

  useEffect(() => {
    if (show) {
      setForm(
        department
          ? {
              name: department.name || '',
              address: department.address || '',
              phoneNumber: department.phoneNumber ? formatBelarusPhone(department.phoneNumber) : '',
              region: department.region || '',
            }
          : initialForm,
      );
      setError('');
    }
  }, [department, show]);

  const validationMessage = useMemo(() => {
    if (!form.name.trim()) return 'Укажите название отделения';
    if (!form.region.trim()) return 'Укажите регион';
    if (form.phoneNumber && !PHONE_PATTERN.test(form.phoneNumber)) {
      return 'Телефон должен быть в формате +375 (29) 498-20-91';
    }
    return '';
  }, [form.name, form.phoneNumber, form.region]);

  const handleChange = ({ target }) => {
    const value = target.name === 'phoneNumber' ? formatBelarusPhone(target.value) : target.value;
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
        <Modal.Title>{department ? 'Редактирование отделения' : 'Новое отделение'}</Modal.Title>
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
            <Col md={6}>
              <Form.Group>
                <Form.Label>Телефон</Form.Label>
                <Form.Control
                  name="phoneNumber"
                  value={form.phoneNumber}
                  onChange={handleChange}
                  placeholder="+375 (29) 498-20-91"
                />
              </Form.Group>
            </Col>
            <Col md={6}>
              <Form.Group>
                <Form.Label>Регион</Form.Label>
                <Form.Control name="region" value={form.region} onChange={handleChange} />
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
            {loading ? 'Сохраняем...' : department ? 'Сохранить' : 'Создать'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
