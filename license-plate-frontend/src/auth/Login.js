import { useMemo, useState } from 'react';
import { Alert, Form, InputGroup, Spinner } from 'react-bootstrap';
import { Link, Navigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from './AuthContext';

const initialForm = {
  identifier: '',
  password: '',
};

const loginHighlights = [
  {
    title: 'Мгновенный доступ',
    text: 'Вход в кабинет занимает пару секунд, а история обращений сразу остается под рукой.',
  },
  {
    title: 'Номер и статус рядом',
    text: 'Номерной знак, этап оформления и отделение видны в одном месте без лишних переходов.',
  },
  {
    title: 'Понятные обновления',
    text: 'Изменения по заявлению и комментарии отображаются прямо в личном кабинете.',
  },
];

function Login() {
  const { login, isAuthenticated, getDefaultRoute, extractErrorMessage, authLoading } = useAuth();
  const [form, setForm] = useState(initialForm);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const isValid = useMemo(() => form.identifier.trim() && form.password.trim(), [form]);

  const handleChange = ({ target }) => {
    setForm((current) => ({ ...current, [target.name]: target.value }));
    setError('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!isValid) {
      setError('Введите логин и пароль');
      return;
    }

    setLoading(true);

    try {
      await login(form);
      toast.success('Вход выполнен');
    } catch (submitError) {
      setError(extractErrorMessage(submitError, 'Не удалось войти'));
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
        <span className="landing-kicker">Вход в сервис</span>
        <h1 className="auth-login-title">Один кабинет для всех заявлений и всех этапов оформления</h1>
        <p className="auth-side-text auth-side-text-wide">
          Входите в личный кабинет, чтобы быстро открыть историю обращений, проверить статус оформления и
          вернуться к выбранному номеру без лишних шагов.
        </p>

        <div className="auth-showcase-grid" role="list">
          {loginHighlights.map((item) => (
            <article key={item.title} className="auth-showcase-card auth-showcase-card-text-only" role="listitem">
              <h3>{item.title}</h3>
              <p>{item.text}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="auth-card auth-card-compact glass-panel auth-form-panel" aria-labelledby="login-title">
        <div className="auth-card-header auth-card-header-tight auth-card-header-redesign">
          <span className="landing-kicker">Вход</span>
          <h2 id="login-title">Войти в систему</h2>
          <p className="auth-subtitle">
            Используйте email или номер паспорта, чтобы открыть личный кабинет.
          </p>
        </div>

        <Form onSubmit={handleSubmit} className="auth-form-grid">
          <Form.Group className="auth-field">
            <Form.Label>Логин</Form.Label>
            <InputGroup>
              <InputGroup.Text>
                <i className="bi bi-person-circle" />
              </InputGroup.Text>
              <Form.Control
                name="identifier"
                placeholder="Например: ivanov@mail.ru или MP1234567"
                value={form.identifier}
                onChange={handleChange}
                autoComplete="username"
                aria-label="Логин"
              />
            </InputGroup>
          </Form.Group>

          <Form.Group className="auth-field">
            <Form.Label>Пароль</Form.Label>
            <InputGroup>
              <InputGroup.Text>
                <i className="bi bi-key" />
              </InputGroup.Text>
              <Form.Control
                name="password"
                type="password"
                placeholder="Введите пароль"
                value={form.password}
                onChange={handleChange}
                autoComplete="current-password"
                aria-label="Пароль"
              />
            </InputGroup>
          </Form.Group>

          {error ? (
            <Alert variant="danger" className="glass-alert">
              {error}
            </Alert>
          ) : null}

          <button type="submit" className="landing-button landing-button-primary auth-submit-button" disabled={loading}>
            {loading ? (
              <>
                <Spinner size="sm" className="me-2" />
                Проверяем...
              </>
            ) : (
              'Войти'
            )}
          </button>
        </Form>

        <div className="auth-footer auth-footer-redesign">
          <span>Нет аккаунта?</span>
          <Link to="/register">Зарегистрироваться</Link>
        </div>
      </section>
    </main>
  );
}

export default Login;
