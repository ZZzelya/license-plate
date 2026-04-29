import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Col, Form, Modal, Row } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { useAuth } from '../auth/AuthContext';
import { extractErrorMessage, passportChangeRequestsApi } from '../common/api';
import { formatBelarusPhone } from '../common/formatters';

const REQUEST_STATUS_LABELS = {
  PENDING: 'На рассмотрении',
  APPROVED: 'Подтверждена',
  REJECTED: 'Отклонена',
};

function PassportRequestModal({ show, onHide, onSubmit, loading, currentPhone, disabled }) {
  const [newPassportNumber, setNewPassportNumber] = useState('');
  const [contactPhone, setContactPhone] = useState(currentPhone || '');
  const [error, setError] = useState('');

  useEffect(() => {
    if (show) {
      setNewPassportNumber('');
      setContactPhone(currentPhone || '');
      setError('');
    }
  }, [currentPhone, show]);

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (disabled) {
      setError('У вас уже есть необработанная заявка на смену паспорта');
      return;
    }

    if (!/^[A-Z]{2}\d{7}$/.test(newPassportNumber.trim().toUpperCase())) {
      setError('Укажите новый номер паспорта в формате MP1234567');
      return;
    }

    if (!contactPhone.trim()) {
      setError('Укажите телефон для связи');
      return;
    }

    await onSubmit({
      newPassportNumber: newPassportNumber.trim().toUpperCase(),
      contactPhone: contactPhone.trim(),
    });
  };

  return (
    <Modal centered show={show} onHide={onHide} dialogClassName="glass-modal">
      <Modal.Header closeButton closeVariant="white" className="modal-dark-header">
        <Modal.Title>Заявка на смену паспорта</Modal.Title>
      </Modal.Header>

      <Form onSubmit={handleSubmit}>
        <Modal.Body>
          <p className="helper-text mb-3">
            После отправки заявка попадет администратору на проверку. Паспорт изменится только после подтверждения.
          </p>

          <Form.Group className="mb-3">
            <Form.Label>Новый номер паспорта</Form.Label>
            <Form.Control
              value={newPassportNumber}
              onChange={(event) => {
                setNewPassportNumber(event.target.value.toUpperCase());
                setError('');
              }}
              placeholder="Например: MP7654321"
            />
          </Form.Group>

          <Form.Group>
            <Form.Label>Телефон для связи</Form.Label>
            <Form.Control
              value={contactPhone}
              onChange={(event) => {
                setContactPhone(formatBelarusPhone(event.target.value));
                setError('');
              }}
              placeholder="+375 (29) 367-45-67"
            />
          </Form.Group>

          {error ? (
            <Alert variant="danger" className="glass-alert mt-3">
              {error}
            </Alert>
          ) : null}
        </Modal.Body>

        <Modal.Footer className="modal-dark-footer">
          <Button variant="outline-light" className="glass-button" onClick={onHide}>
            Отмена
          </Button>
          <Button type="submit" className="primary-button" disabled={loading || disabled}>
            {loading ? 'Отправляем...' : 'Отправить заявку'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}

function UserProfile() {
  const { user, updateProfile, refreshProfile } = useAuth();
  const [form, setForm] = useState({
    fullName: '',
    phoneNumber: '',
    email: '',
    address: '',
  });
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(false);
  const [requestsLoading, setRequestsLoading] = useState(true);
  const [passportLoading, setPassportLoading] = useState(false);
  const [error, setError] = useState('');
  const [showPassportModal, setShowPassportModal] = useState(false);

  const loadPassportRequests = async () => {
    setRequestsLoading(true);

    try {
      const result = await passportChangeRequestsApi.getMine();
      setRequests(result);
    } catch (loadError) {
      toast.error(extractErrorMessage(loadError, 'Не удалось загрузить заявки на смену паспорта'));
    } finally {
      setRequestsLoading(false);
    }
  };

  useEffect(() => {
    refreshProfile();
    loadPassportRequests();
  }, [refreshProfile]);

  useEffect(() => {
    setForm({
      fullName: user?.fullName || '',
      phoneNumber: user?.phoneNumber || '',
      email: user?.email || '',
      address: user?.address || '',
    });
  }, [user]);

  const validationMessage = useMemo(() => {
    if (!form.fullName.trim()) return 'Укажите фамилию, имя и отчество';
    return '';
  }, [form.fullName]);

  const latestRequest = requests[0] || null;
  const hasPendingPassportRequest = requests.some((request) => request.status === 'PENDING');

  const handleChange = ({ target }) => {
    let value = target.value;

    if (target.name === 'phoneNumber') {
      value = formatBelarusPhone(value);
    }

    setForm((current) => ({ ...current, [target.name]: value }));
    setError('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (validationMessage) {
      setError(validationMessage);
      return;
    }

    setLoading(true);

    try {
      await updateProfile({
        fullName: form.fullName,
        phoneNumber: form.phoneNumber,
        email: form.email,
        address: form.address,
      });
      toast.success('Контактные данные обновлены');
    } catch (submitError) {
      setError(extractErrorMessage(submitError, 'Не удалось обновить профиль'));
    } finally {
      setLoading(false);
    }
  };

  const handlePassportRequest = async ({ newPassportNumber, contactPhone }) => {
    setPassportLoading(true);

    try {
      await passportChangeRequestsApi.create({
        newPassportNumber,
        contactPhone,
      });
      setShowPassportModal(false);
      await loadPassportRequests();
      toast.success('Заявка на смену паспорта отправлена администратору');
    } catch (submitError) {
      toast.error(extractErrorMessage(submitError, 'Не удалось отправить заявку'));
    } finally {
      setPassportLoading(false);
    }
  };

  return (
    <>
      <section className="page-header">
        <div className="page-title">
          <p className="eyebrow">Мой профиль</p>
          <h1>Ваши контактные данные</h1>
          <p>Обновляйте контакты и подавайте заявку на смену паспорта, если данные изменились.</p>
        </div>
      </section>

      <section className="profile-layout">
        <article className="profile-summary glass-panel">
          <div className="profile-summary-top">
            <div className="profile-avatar-lg">
              {user?.fullName
                ?.split(' ')
                .filter(Boolean)
                .slice(0, 2)
                .map((part) => part[0])
                .join('')
                .toUpperCase() || 'ЛК'}
            </div>
            <div>
              <h2>{user?.fullName || 'Пользователь'}</h2>
              <p>{user?.email || 'Электронная почта не указана'}</p>
            </div>
          </div>

          <div className="profile-facts">
            <div className="detail-card detail-card-accent">
              <div className="detail-label">Паспортные данные</div>
              <div className="detail-value">{user?.passportNumber || 'Не указан'}</div>
              <div className="detail-note">
                Паспорт меняется через отдельную заявку. После отправки ее проверяет администратор.
              </div>
            </div>

            <div className="detail-card">
              <div className="detail-label">Последняя заявка на смену паспорта</div>
              {requestsLoading ? (
                <div className="detail-value">Загрузка...</div>
              ) : latestRequest ? (
                <>
                  <div className="detail-value">{REQUEST_STATUS_LABELS[latestRequest.status] || latestRequest.status}</div>
                  <div className="detail-note">
                    Новый паспорт: {latestRequest.requestedPassportNumber}
                    {latestRequest.adminComment ? ` • ${latestRequest.adminComment}` : ''}
                  </div>
                </>
              ) : (
                <div className="detail-value">Заявок пока нет</div>
              )}
            </div>
          </div>

          <Button
            className="glass-button mt-3 profile-passport-button"
            onClick={() => setShowPassportModal(true)}
            disabled={hasPendingPassportRequest}
          >
            <i className="bi bi-arrow-repeat" />
            {hasPendingPassportRequest ? 'Заявка уже отправлена' : 'Подать заявку на смену паспорта'}
          </Button>
        </article>

        <article className="profile-edit glass-panel">
          <div className="section-title-row">
            <div>
              <h2 className="section-title">Ваши контактные данные</h2>
              <p className="helper-text mb-0">
                Эти данные используются для связи по заявлениям и административным вопросам.
              </p>
            </div>
          </div>

          <Form onSubmit={handleSubmit}>
            <Row className="g-3">
              <Col md={6}>
                <Form.Group>
                  <Form.Label>ФИО</Form.Label>
                  <Form.Control
                    name="fullName"
                    value={form.fullName}
                    onChange={handleChange}
                    placeholder="Например: Иванов Иван Иванович"
                  />
                </Form.Group>
              </Col>

              <Col md={6}>
                <Form.Group>
                  <Form.Label>Паспорт</Form.Label>
                  <Form.Control value={user?.passportNumber || ''} readOnly />
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

              <Col md={6}>
                <Form.Group>
                  <Form.Label>Email</Form.Label>
                  <Form.Control
                    name="email"
                    type="email"
                    value={form.email}
                    onChange={handleChange}
                    placeholder="Например: ivanov@mail.ru"
                  />
                </Form.Group>
              </Col>

              <Col md={12}>
                <Form.Group>
                  <Form.Label>Адрес проживания</Form.Label>
                  <Form.Control
                    name="address"
                    value={form.address}
                    onChange={handleChange}
                    placeholder="Например: г. Минск, ул. Лесная, д. 12, кв. 45"
                  />
                </Form.Group>
              </Col>
            </Row>

            {error ? (
              <Alert variant="danger" className="glass-alert mt-3">
                {error}
              </Alert>
            ) : null}

            <div className="page-actions mt-4">
              <Button type="submit" className="primary-button" disabled={loading}>
                {loading ? 'Сохраняем...' : 'Сохранить изменения'}
              </Button>
            </div>
          </Form>
        </article>
      </section>

      <PassportRequestModal
        show={showPassportModal}
        onHide={() => setShowPassportModal(false)}
        onSubmit={handlePassportRequest}
        loading={passportLoading}
        currentPhone={form.phoneNumber}
        disabled={hasPendingPassportRequest}
      />
    </>
  );
}

export default UserProfile;
