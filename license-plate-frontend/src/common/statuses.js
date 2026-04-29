export const APPLICATION_STATUS_LABELS = {
  PENDING: 'На рассмотрении',
  CONFIRMED: 'Подтверждено',
  COMPLETED: 'Завершено',
  CANCELLED: 'Отменено',
  REJECTED: 'Отклонено',
  EXPIRED: 'Истек срок действия',
};

export const PLATE_STATUS_LABELS = {
  AVAILABLE: 'Доступен',
  RESERVED: 'Зарезервирован',
  ISSUED: 'Выдан',
};

export const SERVICE_STATUS_LABELS = {
  AVAILABLE: 'Доступна',
  DISABLED: 'Недоступна',
};

export const ROLE_LABELS = {
  USER: '',
  ADMIN: 'Администратор',
};

export const getApplicationStatusLabel = (status) =>
  APPLICATION_STATUS_LABELS[status] || status || 'Не указан';

export const getPlateStatusLabel = (status) =>
  PLATE_STATUS_LABELS[status] || status || 'Не указан';

export const getServiceStatusLabel = (status) =>
  SERVICE_STATUS_LABELS[status] || status || 'Не указан';

export const getRoleLabel = (role) => ROLE_LABELS[role] ?? role ?? '';
