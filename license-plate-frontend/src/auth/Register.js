import { useMemo, useState } from 'react';
import { Alert, Form, InputGroup, Spinner } from 'react-bootstrap';
import { Link, Navigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { formatBelarusPhone } from '../common/formatters';
import { useAuth } from './AuthContext';

const initialForm = {
  fullName: '',
  passportNumber: '',
  email: '',
  phoneNumber: '',
  address: '',
  password: '',
  confirmPassword: '',
};

const registerSteps = [
  'Укажите паспортные и контактные данные',
  'Подтвердите регистрацию паролем',
  'Подавайте и отслеживайте заявления онлайн',
];

function Register() {
  const { register, isAuthenticated, getDefaultRoute, extractErrorMessage, authLoading } = useAuth();
  const [form, setForm] = useState(initialForm);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const validationMessage = useMemo(() => {
    if (!form.fullName.trim()) return 'Укажите фамилию, имя и отчество';
    if (!/^[A-Z]{2}\d{7}$/.test(form.passportNumber.trim().toUpperCase())) {
      return 'Паспорт должен быть в формате MP1234567';
    }
    if (!form.email.trim()) return 'Укажите адрес электронной почты';
    if (form.password.length < 6) return 'Пароль должен быть не короче 6 символов';
    if (form.password !== form.confirmPassword) return 'Пароли не совпадают';
    return '';
  }, [form]);

  const handleChange = ({ target }) => {
    let value = target.value;

    if (target.name === 'passportNumber') {
      value = value.toUpperCase();
    }

    if (target.name === 'phoneNumber') {
      value = formatBelarusPhone(value);
    }

    setForm((current) => ({
      ...current,
      [target.name]: value,
    }));
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
      await register(form);
      toast.success('Регистрация прошла успешно');
    } catch (submitError) {
      setError(extractErrorMessage(submitError, 'Не удалось зарегистрироваться'));
    } finally {
      setLoading(false);
    }
  };

  if (!authLoading && isAuthenticated) {
    return <Navigate to={getDefaultRoute()} replace />;
  }

  return (
    <main className="auth-layout auth-layout-split auth-layout-redesign">
      <section className="auth-side-card glass-panel auth-side-card-redesign">
        <span className="landing-kicker">Регистрация заявителя</span>
        <h1 className="auth-login-title">Создайте кабинет для подачи заявлений и быстрого управления оформлением</h1>
        <p className="auth-side-text auth-side-text-wide">
          После регистрации вы сможете выбрать номерной знак, подать заявление онлайн и хранить контакты,
          обращения и статусы в одном аккуратном интерфейсе.
        </p>

        <div className="auth-steps-list" role="list">
          {registerSteps.map((step, index) => (
            <div key={step} className="auth-step-item" role="listitem">
              <span className="auth-step-index" aria-hidden="true">
                {index + 1}
              </span>
              <span>{step}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="auth-card glass-panel auth-form-panel auth-form-panel-wide" aria-labelledby="register-title">
        <div className="auth-card-header auth-card-header-tight auth-card-header-redesign">
          <span className="landing-kicker">Регистрация</span>
          <h2 id="register-title">Заполните данные для создания кабинета</h2>
          <p className="auth-subtitle">Мы используем эти данные для входа, связи по заявлениям и личного кабинета.</p>
        </div>

        <Form onSubmit={handleSubmit} className="auth-form-grid auth-form-grid-two-columns">
          <Form.Group className="auth-field">
            <Form.Label>ФИО</Form.Label>
            <InputGroup>
              <InputGroup.Text>
                <i className="bi bi-person-vcard" />
              </InputGroup.Text>
              <Form.Control
                name="fullName"
                placeholder="Например: Иванов Иван Иванович"
                value={form.fullName}
                onChange={handleChange}
                aria-label="ФИО"
              />
            </InputGroup>
          </Form.Group>

          <Form.Group className="auth-field">
            <Form.Label>Паспорт</Form.Label>
            <InputGroup>
              <InputGroup.Text>
                <i className="bi bi-postcard" />
              </InputGroup.Text>
              <Form.Control
                name="passportNumber"
                placeholder="Например: MP1234567"
                value={form.passportNumber}
                onChange={handleChange}
                aria-label="Паспорт"
              />
            </InputGroup>
          </Form.Group>

          <Form.Group className="auth-field">
            <Form.Label>Email</Form.Label>
            <InputGroup>
              <InputGroup.Text>
                <i className="bi bi-envelope" />
              </InputGroup.Text>
              <Form.Control
                name="email"
                type="email"
                placeholder="Например: ivanov@mail.ru"
                value={form.email}
                onChange={handleChange}
                aria-label="Email"
              />
            </InputGroup>
          </Form.Group>

          <Form.Group className="auth-field">
            <Form.Label>Телефон</Form.Label>
            <InputGroup>
              <InputGroup.Text>
                <i className="bi bi-telephone" />
              </InputGroup.Text>
              <Form.Control
                name="phoneNumber"
                placeholder="+375 (29) 367-45-67"
                value={form.phoneNumber}
                onChange={handleChange}
                aria-label="Телефон"
              />
            </InputGroup>
          </Form.Group>

          <Form.Group className="auth-field auth-field-full">
            <Form.Label>Адрес проживания</Form.Label>
            <InputGroup>
              <InputGroup.Text>
                <i className="bi bi-geo-alt" />
              </InputGroup.Text>
              <Form.Control
                name="address"
                placeholder="Например: г. Минск, ул. Лесная, д. 12, кв. 45"
                value={form.address}
                onChange={handleChange}
                aria-label="Адрес проживания"
              />
            </InputGroup>
          </Form.Group>

          <Form.Group className="auth-field">
            <Form.Label>Пароль</Form.Label>
            <InputGroup>
              <InputGroup.Text>
                <i className="bi bi-lock" />
              </InputGroup.Text>
              <Form.Control
                name="password"
                type="password"
                placeholder="Не менее 6 символов"
                value={form.password}
                onChange={handleChange}
                aria-label="Пароль"
              />
            </InputGroup>
          </Form.Group>

          <Form.Group className="auth-field">
            <Form.Label>Повтор пароля</Form.Label>
            <InputGroup>
              <InputGroup.Text>
                <i className="bi bi-lock-fill" />
              </InputGroup.Text>
              <Form.Control
                name="confirmPassword"
                type="password"
                placeholder="Повторите пароль"
                value={form.confirmPassword}
                onChange={handleChange}
                aria-label="Повтор пароля"
              />
            </InputGroup>
          </Form.Group>

          {error ? (
            <Alert variant="danger" className="glass-alert auth-field-full">
              {error}
            </Alert>
          ) : null}

          <div className="auth-field-full">
            <button type="submit" className="landing-button landing-button-primary auth-submit-button" disabled={loading}>
              {loading ? (
                <>
                  <Spinner size="sm" className="me-2" />
                  Создаем кабинет...
                </>
              ) : (
                'Зарегистрироваться'
              )}
            </button>
          </div>
        </Form>

        <div className="auth-footer auth-footer-redesign">
          <span>Уже есть аккаунт?</span>
          <Link to="/login">Перейти ко входу</Link>
        </div>
      </section>
    </main>
  );
}

export default Register;
