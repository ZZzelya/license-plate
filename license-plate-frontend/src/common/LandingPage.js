import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const WORKFLOW_STEPS = [
  {
    id: 1,
    icon: 'bi-person-vcard',
    title: 'Создайте кабинет',
    description: 'Заполните данные один раз и используйте их при следующих обращениях без повторного ввода.',
  },
  {
    id: 2,
    icon: 'bi-grid-3x3-gap',
    title: 'Выберите номер и сценарий',
    description: 'Случайная выдача, выбор из доступных в ГАИ или персонализированный номер.',
  },
  {
    id: 3,
    icon: 'bi-pencil-square',
    title: 'Подайте заявление онлайн',
    description: 'Система сразу покажет отделение, выбранные услуги и все параметры оформления.',
  },
  {
    id: 4,
    icon: 'bi-shield-check',
    title: 'Следите за этапами',
    description: 'Статус, комментарии и подтверждение видны в личном кабинете в одном месте.',
  },
];

const BENEFITS = [
  {
    icon: 'bi-lightning-charge',
    title: 'Быстрый старт',
    text: 'Оформление начинается сразу: личные данные, сценарий получения и заявление собраны в один понятный поток.',
  },
  {
    icon: 'bi-diagram-3',
    title: 'Прозрачный процесс',
    text: 'На каждом шаге видно, что уже выбрано, какой номер будет выдан и на каком этапе находится обращение.',
  },
  {
    icon: 'bi-sliders2',
    title: 'Гибкий выбор',
    text: 'Можно получить случайный номер, выбрать доступный вариант или подключить персонализированный формат.',
  },
];

const DASHBOARD_ITEMS = [
  {
    icon: 'bi-file-earmark-text',
    title: 'История заявлений',
    text: 'Все обращения хранятся в одном кабинете с датами, статусами и карточками оформления.',
  },
  {
    icon: 'bi-postcard',
    title: 'Выбранный номер',
    text: 'Номер, регион, отделение и подключенные услуги всегда видны без лишнего поиска.',
  },
  {
    icon: 'bi-chat-square-text',
    title: 'Комментарии и статусы',
    text: 'Подтверждение, отмена или уточнения показываются прямо в интерфейсе без звонков и бумажных уведомлений.',
  },
];

function Hero({ steps, activeStep, onStepChange, theme, toggleTheme }) {
  const progress = ((activeStep + 1) / steps.length) * 100;
  const currentStep = steps[activeStep];

  return (
    <section className="landing-hero" aria-labelledby="landing-title">
      <div className="landing-hero-copy">
        <div className="landing-hero-topline">
          <span className="landing-kicker">Система получения автомобильных номерных знаков</span>
          <button type="button" className="landing-theme-switch" onClick={toggleTheme}>
            <i className={`bi ${theme === 'dark' ? 'bi-sunrise' : 'bi-moon-stars'}`} />
            <span>{theme === 'dark' ? 'Светлая тема' : 'Тёмная тема'}</span>
          </button>
        </div>

        <h1 id="landing-title">Быстрое оформление без очередей, лишнего текста и бумажной рутины</h1>
        <p className="landing-lead">
          Подавайте заявление онлайн, выбирайте доступный номер и контролируйте весь процесс через один аккуратный
          личный кабинет.
        </p>

        <div className="landing-hero-actions" role="group" aria-label="Основные действия">
          <Link to="/register" className="landing-button landing-button-primary">
            Зарегистрироваться
          </Link>
          <Link to="/login" className="landing-button landing-button-secondary">
            Войти
          </Link>
        </div>

        <div className="landing-progress-card" aria-label="Прогресс оформления">
          <div className="landing-progress-head">
            <div>
              <strong>Путь оформления</strong>
              <span>
                Шаг {currentStep.id} из {steps.length}
              </span>
            </div>
            <span className="landing-progress-badge">
              <i className={`bi ${currentStep.icon}`} />
            </span>
          </div>

          <div className="landing-progress-track" aria-hidden="true">
            <div className="landing-progress-fill" style={{ width: `${progress}%` }} />
          </div>

          <div className="landing-step-pills" role="tablist" aria-label="Этапы оформления">
            {steps.map((step, index) => (
              <button
                key={step.id}
                type="button"
                role="tab"
                tabIndex={0}
                aria-selected={activeStep === index}
                className={`landing-step-pill ${activeStep === index ? 'is-active' : ''}`}
                onClick={() => onStepChange(index)}
              >
                <span aria-hidden="true">
                  <i className={`bi ${step.icon}`} />
                </span>
                <span>{step.title}</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      <aside className="landing-hero-panel" aria-label="Ключевой сценарий">
        <div className="landing-hero-panel-card">
          <span className="landing-soft-label">Быстрый вход в сервис</span>
          <h2>{currentStep.title}</h2>
          <p>{currentStep.description}</p>

          <div className="landing-panel-highlights">
            <div className="landing-panel-highlight">
              <strong>Онлайн-подача</strong>
              <span>Без повторного заполнения анкет и лишних поездок по кабинетам.</span>
            </div>
            <div className="landing-panel-highlight">
              <strong>Доступные номера</strong>
              <span>Система показывает понятные сценарии выдачи и выбора номерного знака.</span>
            </div>
            <div className="landing-panel-highlight">
              <strong>Контроль статуса</strong>
              <span>Все этапы, комментарии и результат собраны внутри кабинета.</span>
            </div>
          </div>
        </div>
      </aside>
    </section>
  );
}

function Benefits() {
  return (
    <section className="landing-section" aria-labelledby="benefits-title">
      <div className="landing-section-heading">
        <span className="landing-section-kicker">Быстрое оформление</span>
        <h2 id="benefits-title">Три причины, почему сервис читается легко и ведет к действию</h2>
      </div>

      <div className="landing-benefits-grid">
        {BENEFITS.map((item) => (
          <article key={item.title} className="landing-feature-card">
            <div className="landing-feature-icon" aria-hidden="true">
              <i className={`bi ${item.icon}`} />
            </div>
            <h3>{item.title}</h3>
            <p>{item.text}</p>
          </article>
        ))}
      </div>
    </section>
  );
}

function DashboardPreview() {
  return (
    <section className="landing-section" aria-labelledby="dashboard-preview-title">
      <div className="landing-dashboard-card">
        <div className="landing-dashboard-copy">
          <span className="landing-section-kicker">Что внутри кабинета</span>
          <h2 id="dashboard-preview-title">Один экран для статусов, номера и всех обращений</h2>
          <p>
            Вместо сплошного текста пользователь получает карточки со статусами, выбранным номером, историей
            обращений и понятными следующими действиями.
          </p>
        </div>

        <div className="landing-dashboard-grid" role="list">
          {DASHBOARD_ITEMS.map((item) => (
            <article key={item.title} className="landing-dashboard-item" role="listitem">
              <span className="landing-dashboard-icon" aria-hidden="true">
                <i className={`bi ${item.icon}`} />
              </span>
              <div>
                <h3>{item.title}</h3>
                <p>{item.text}</p>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

function LandingPage() {
  const { isAuthenticated, getDefaultRoute, authLoading, theme, toggleTheme } = useAuth();
  const [activeStep, setActiveStep] = useState(0);

  if (!authLoading && isAuthenticated) {
    return <Navigate to={getDefaultRoute()} replace />;
  }

  return (
    <main className="landing-shell landing-redesign-shell">
      <div className="landing-orb landing-orb-left" aria-hidden="true" />
      <div className="landing-orb landing-orb-right" aria-hidden="true" />

      <div className="landing-redesign-card glass-panel">
        <Hero
          steps={WORKFLOW_STEPS}
          activeStep={activeStep}
          onStepChange={setActiveStep}
          theme={theme}
          toggleTheme={toggleTheme}
        />
        <Benefits />
        <DashboardPreview />
      </div>
    </main>
  );
}

export default LandingPage;
