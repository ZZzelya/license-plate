import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Form, Modal, Spinner } from 'react-bootstrap';
import { licensePlatesApi, servicesApi } from '../common/api';
import {
  isAvailablePlateServiceRule,
  isDuplicatePlateServiceRule,
  isKeepPlateServiceRule,
  isPersonalizedPlateServiceRule,
  resolveAllowedRegionCodesRule,
} from '../common/plateRules';
import { getApplicationStatusLabel } from '../common/statuses';

const initialForm = {
  departmentId: '',
  region: '',
  plateNumber: '',
  vehicleType: '',
  vehicleVin: '',
  vehicleModel: '',
  vehicleYear: '',
  notes: '',
  serviceIds: [],
};

const VIN_REGEX = /^[A-Z0-9]{17}$/;
const PLATE_REGEX = /^(?:\d{4}\s[ABEKMHOPCTX]{2}-\d|E\d{3}\s[A-Z]{2}-\d)$/;
const MODEL_REGEX = /^[A-ZА-ЯЁ0-9][A-ZА-ЯЁ0-9\s\-./]{1,59}$/i;
const NOTES_MAX_LENGTH = 300;

const formatVin = (value = '') => value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 17);

const formatPersonalizedPlate = (value = '') => {
  const normalized = value.toUpperCase();
  if (normalized.startsWith('E')) {
    const cleaned = normalized.replace(/[^A-Z0-9]/g, '').slice(0, 6);
    const digits = cleaned.slice(1, 4).replace(/\D/g, '');
    const letters = cleaned.slice(4, 6).replace(/[^A-Z]/g, '');
    const regionDigit = normalized.replace(/[^A-Z0-9]/g, '').slice(6, 7).replace(/\D/g, '');

    let formatted = `E${digits}`;
    if (letters) {
      formatted += `${digits.length === 3 ? ' ' : ''}${letters}`;
    }
    if (regionDigit) {
      formatted += `${digits.length === 3 && letters.length === 2 ? '-' : ''}${regionDigit}`;
    }
    return formatted;
  }

  const cleaned = normalized.replace(/[^A-Z0-9]/g, '').slice(0, 7);
  const digits = cleaned.slice(0, 4).replace(/\D/g, '');
  const letters = cleaned.slice(4, 6).replace(/[^ABEKMHOPCTX]/g, '');
  const regionDigit = cleaned.slice(6, 7).replace(/\D/g, '');

  let formatted = digits;

  if (letters) {
    formatted += `${digits.length === 4 ? ' ' : ''}${letters}`;
  }

  if (regionDigit) {
    formatted += `${digits.length === 4 && letters.length === 2 ? '-' : ''}${regionDigit}`;
  }

  return formatted;
};

const normalizeNotes = (value = '') => value.replace(/\s+/g, ' ').trimStart().slice(0, NOTES_MAX_LENGTH);

const getPlateView = (plate) =>
  plate?.plateNumber ||
  [plate?.numberPart, plate?.series && plate?.regionCode ? `${plate.series}-${plate.regionCode}` : null]
    .filter(Boolean)
    .join(' ');

const isElectricPlateNumber = (value = '') => value.toUpperCase().startsWith('E');

const getServiceDescription = (service) => {
  if (isAvailablePlateServiceRule(service)) {
    return 'Выбор из предложенных номеров';
  }

  return service.description || 'Без описания';
};

const validateStepOne = (form, passportNumber) => {
  const nextErrors = {};
  const currentYear = new Date().getFullYear() + 1;

  if (!passportNumber) {
    nextErrors.form = 'Для создания заявления нужен паспорт текущего пользователя.';
  }

  if (!form.departmentId) {
    nextErrors.departmentId = 'Выберите отделение.';
  }

  if (!form.vehicleType) {
    nextErrors.vehicleType = 'Выберите тип автомобиля.';
  }

  if (!form.vehicleVin) {
    nextErrors.vehicleVin = 'Введите VIN.';
  } else if (!VIN_REGEX.test(form.vehicleVin)) {
    nextErrors.vehicleVin = 'VIN должен содержать 17 латинских букв и цифр.';
  }

  if (!form.vehicleModel.trim()) {
    nextErrors.vehicleModel = 'Укажите модель автомобиля.';
  } else if (!MODEL_REGEX.test(form.vehicleModel.trim())) {
    nextErrors.vehicleModel = 'Модель содержит недопустимые символы.';
  }

  if (!form.vehicleYear.trim()) {
    nextErrors.vehicleYear = 'Укажите год выпуска.';
  } else {
    const year = Number(form.vehicleYear);
    if (!Number.isInteger(year) || year < 1900 || year > currentYear) {
      nextErrors.vehicleYear = `Год выпуска должен быть в диапазоне 1900-${currentYear}.`;
    }
  }

  if (form.notes.trim().length > NOTES_MAX_LENGTH) {
    nextErrors.notes = `Комментарий не должен превышать ${NOTES_MAX_LENGTH} символов.`;
  }

  return nextErrors;
};

const validateStepTwo = (form, selectionMode, availableRegionCodes, selectedServices = [], checkedPlate = null) => {
  const nextErrors = {};

  if (selectionMode === 'random') {
    return nextErrors;
  }

  if (!form.plateNumber.trim()) {
    nextErrors.plateNumber =
      selectionMode === 'personalized'
        ? 'Введите персонализированный номер.'
        : 'Выберите номер из доступных.';
    return nextErrors;
  }

  if (selectionMode === 'personalized') {
    if (!PLATE_REGEX.test(form.plateNumber.trim())) {
      nextErrors.plateNumber = 'Укажите номер по шаблону: 3256 XX-2 или E000 AA-7.';
      return nextErrors;
    }

    const electricPlate = isElectricPlateNumber(form.plateNumber.trim());
    if (form.vehicleType === 'ELECTRIC' && !electricPlate) {
      nextErrors.plateNumber = 'Для электромобиля нужен номер формата E000 AA-7.';
      return nextErrors;
    }
    if (form.vehicleType === 'STANDARD' && electricPlate) {
      nextErrors.plateNumber = 'Для обычного автомобиля нужен номер формата 3256 XX-2.';
      return nextErrors;
    }

    const regionCode = form.plateNumber.trim().slice(-1);
    if (availableRegionCodes.length && !availableRegionCodes.includes(regionCode)) {
      nextErrors.plateNumber = `Код региона должен соответствовать отделению: ${availableRegionCodes.join(' или ')}.`;
    }
  }

  if (selectionMode === 'existing') {
    if (!PLATE_REGEX.test(form.plateNumber.trim())) {
      nextErrors.plateNumber = 'Укажите номер по шаблону: 3256 XX-2 или E000 AA-7.';
      return nextErrors;
    }

    if (!checkedPlate) {
      nextErrors.plateNumber = 'Номерной знак должен существовать в базе.';
      return nextErrors;
    }

    if (selectedServices.some(isDuplicatePlateServiceRule) && !checkedPlate.issueDate) {
      nextErrors.plateNumber = 'Для изготовления дубликатов можно использовать только ранее выданный номер.';
    }
  }

  return nextErrors;
};

function FieldError({ message }) {
  if (!message) {
    return null;
  }

  return <Form.Text className="text-danger d-block mt-1">{message}</Form.Text>;
}

export function ApplicationWizardModal({
  show,
  onHide,
  onSubmit,
  services = [],
  departments = [],
  passportNumber = '',
  activePlate = '',
  loading = false,
}) {
  const [step, setStep] = useState(1);
  const [form, setForm] = useState(initialForm);
  const [fieldErrors, setFieldErrors] = useState({});
  const [availablePlates, setAvailablePlates] = useState([]);
  const [checkedPlate, setCheckedPlate] = useState(null);
  const [loadingPlates, setLoadingPlates] = useState(false);
  const [error, setError] = useState('');

  const selectedServices = useMemo(
    () => services.filter((service) => form.serviceIds.includes(service.id)),
    [form.serviceIds, services],
  );

  const selectedDepartment = useMemo(
    () => departments.find((department) => department.id === Number(form.departmentId)),
    [departments, form.departmentId],
  );

  const availableRegionCodes = useMemo(
    () => resolveAllowedRegionCodesRule(selectedDepartment?.region || form.region),
    [selectedDepartment?.region, form.region],
  );

  const selectionMode = useMemo(() => {
    const hasAvailableSelection = selectedServices.some(isAvailablePlateServiceRule);
    const hasPersonalizedSelection = selectedServices.some(isPersonalizedPlateServiceRule);
    const hasExistingPlateSelection = selectedServices.some(
      (service) => isDuplicatePlateServiceRule(service) || isKeepPlateServiceRule(service),
    );

    if (hasPersonalizedSelection) return 'personalized';
    if (hasAvailableSelection) return 'available';
    if (hasExistingPlateSelection) return 'existing';
    return 'random';
  }, [selectedServices]);

  const filteredAvailablePlates = useMemo(
    () =>
      availablePlates.filter((plate) =>
        form.vehicleType === 'ELECTRIC'
          ? isElectricPlateNumber(getPlateView(plate))
          : form.vehicleType === 'STANDARD'
            ? !isElectricPlateNumber(getPlateView(plate))
            : false,
      ),
    [availablePlates, form.vehicleType],
  );

  const selectedPlate = useMemo(
    () => filteredAvailablePlates.find((plate) => getPlateView(plate) === form.plateNumber),
    [filteredAvailablePlates, form.plateNumber],
  );

  useEffect(() => {
    if (!show) {
      return;
    }

    setStep(1);
    setForm(initialForm);
    setFieldErrors({});
    setAvailablePlates([]);
    setCheckedPlate(null);
    setError('');
  }, [show]);

  useEffect(() => {
    if (selectedDepartment?.region === undefined) {
      return;
    }

    setForm((current) => ({
      ...current,
      region: selectedDepartment?.region || '',
    }));
  }, [selectedDepartment]);

  useEffect(() => {
    let cancelled = false;

    const loadPlates = async () => {
      if (!form.departmentId) {
        setAvailablePlates([]);
        return;
      }

      setLoadingPlates(true);

      try {
        const result = await licensePlatesApi.getAvailableByDepartment(Number(form.departmentId));
        if (!cancelled) {
          setAvailablePlates(result);
        }
      } catch {
        if (!cancelled) {
          setAvailablePlates([]);
          setError('Не удалось загрузить доступные номерные знаки.');
        }
      } finally {
        if (!cancelled) {
          setLoadingPlates(false);
        }
      }
    };

    loadPlates();

    return () => {
      cancelled = true;
    };
  }, [form.departmentId]);

  useEffect(() => {
    if (selectionMode === 'random' && form.plateNumber) {
      setForm((current) => ({ ...current, plateNumber: '' }));
    }
  }, [form.plateNumber, selectionMode]);

  useEffect(() => {
    let cancelled = false;

    const verifyPlate = async () => {
      if (selectionMode !== 'existing' || !form.plateNumber.trim() || !PLATE_REGEX.test(form.plateNumber.trim())) {
        setCheckedPlate(null);
        return;
      }

      try {
        const result = await licensePlatesApi.getByNumber(form.plateNumber.trim());
        if (!cancelled) {
          setCheckedPlate(result);
        }
      } catch {
        if (!cancelled) {
          setCheckedPlate(null);
        }
      }
    };

    verifyPlate();

    return () => {
      cancelled = true;
    };
  }, [form.plateNumber, selectionMode]);

  const handleFieldChange = ({ target }) => {
    const getPlateInputValue = () => {
      if (selectionMode !== 'personalized') {
        return target.value.toUpperCase();
      }

      if (form.vehicleType === 'ELECTRIC') {
        const nextValue = target.value.toUpperCase().startsWith('E') ? target.value : `E${target.value}`;
        return formatPersonalizedPlate(nextValue);
      }

      if (form.vehicleType === 'STANDARD') {
        return formatPersonalizedPlate(target.value.replace(/^E/i, ''));
      }

      return formatPersonalizedPlate(target.value);
    };

    const value =
      target.name === 'vehicleYear'
        ? target.value.replace(/[^\d]/g, '').slice(0, 4)
        : target.name === 'vehicleVin'
          ? formatVin(target.value)
          : target.name === 'plateNumber'
              ? getPlateInputValue()
              : target.name === 'notes'
                ? normalizeNotes(target.value)
                : target.value;

    setForm((current) => ({
      ...current,
      [target.name]: value,
      ...(target.name === 'departmentId' || target.name === 'vehicleType' ? { plateNumber: '' } : {}),
    }));
    if (target.name === 'plateNumber' || target.name === 'departmentId' || target.name === 'vehicleType') {
      setCheckedPlate(null);
    }
    setFieldErrors((current) => ({ ...current, [target.name]: '' }));
    setError('');
  };

  const toggleService = (serviceId) => {
    const toggledService = services.find((service) => service.id === serviceId);

    setForm((current) => ({
      ...current,
      serviceIds: current.serviceIds.includes(serviceId)
        ? current.serviceIds.filter((id) => id !== serviceId)
        : [
            ...current.serviceIds.filter((id) => {
              const selectedService = services.find((service) => service.id === id);

              if (isAvailablePlateServiceRule(toggledService) && isPersonalizedPlateServiceRule(selectedService)) {
                return false;
              }

              if (isPersonalizedPlateServiceRule(toggledService) && isAvailablePlateServiceRule(selectedService)) {
                return false;
              }

              if (
                (isDuplicatePlateServiceRule(toggledService) || isKeepPlateServiceRule(toggledService)) &&
                (isAvailablePlateServiceRule(selectedService) || isPersonalizedPlateServiceRule(selectedService))
              ) {
                return false;
              }

              if (
                (isAvailablePlateServiceRule(toggledService) || isPersonalizedPlateServiceRule(toggledService)) &&
                (isDuplicatePlateServiceRule(selectedService) || isKeepPlateServiceRule(selectedService))
              ) {
                return false;
              }

              return true;
            }),
            serviceId,
          ],
    }));

    setError('');
  };

  const goNext = () => {
    if (step === 1) {
      const nextErrors = validateStepOne(form, passportNumber);
      setFieldErrors(nextErrors);

      if (Object.keys(nextErrors).length) {
        setError(nextErrors.form || 'Проверьте заполнение полей шага.');
        return;
      }
    }

    if (step === 2) {
      const nextErrors = validateStepTwo(form, selectionMode, availableRegionCodes, selectedServices, checkedPlate);
      setFieldErrors(nextErrors);

      if (Object.keys(nextErrors).length) {
        setError('Проверьте данные номера и выбранного сценария.');
        return;
      }
    }

    setFieldErrors({});
    setError('');
    setStep((current) => Math.min(current + 1, 3));
  };

  const goBack = () => {
    setError('');
    setFieldErrors({});
    setStep((current) => Math.max(current - 1, 1));
  };

  const handleSubmit = async () => {
    const stepOneErrors = validateStepOne(form, passportNumber);
    const stepTwoErrors = validateStepTwo(form, selectionMode, availableRegionCodes, selectedServices, checkedPlate);
    const nextErrors = { ...stepOneErrors, ...stepTwoErrors };

    if (Object.keys(nextErrors).length) {
      setFieldErrors(nextErrors);
      setError('Перед отправкой исправьте ошибки в заявлении.');
      if (Object.keys(stepOneErrors).length) {
        setStep(1);
      } else if (Object.keys(stepTwoErrors).length) {
        setStep(2);
      }
      return;
    }

    await onSubmit({
      passportNumber,
      departmentId: Number(form.departmentId),
      region: selectedDepartment?.region || form.region,
      plateNumber: form.plateNumber.trim() || null,
      vehicleType: form.vehicleType,
      vehicleVin: form.vehicleVin,
      vehicleModel: form.vehicleModel.trim(),
      vehicleYear: form.vehicleYear ? Number(form.vehicleYear) : null,
      notes: form.notes.trim(),
      serviceIds: form.serviceIds,
    });
  };

  return (
    <Modal centered show={show} onHide={onHide} dialogClassName="glass-modal modal-xl">
      <Modal.Header closeButton closeVariant="white" className="modal-dark-header">
        <Modal.Title>Новое заявление</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {activePlate ? (
          <Alert variant="warning" className="glass-alert warning-alert">
            <strong>У вас уже есть активный номерной знак: {activePlate}.</strong> При подаче нового заявления он
            может быть изменен после обработки обращения.
          </Alert>
        ) : null}
        <div className="stepper">
          {[
            { id: 1, label: 'Данные авто' },
            { id: 2, label: 'Номер и услуги' },
            { id: 3, label: 'Подтверждение' },
          ].map((item) => (
            <div key={item.id} className={`step-item ${step === item.id ? 'active' : ''}`}>
              <span className="step-number">{item.id}</span>
              <span>{item.label}</span>
            </div>
          ))}
        </div>

        {step === 1 ? (
          <div className="wizard-grid">
            <div className="detail-grid">
              <Form.Group>
                <Form.Label>Отделение</Form.Label>
                <Form.Select
                  name="departmentId"
                  value={form.departmentId}
                  onChange={handleFieldChange}
                  isInvalid={Boolean(fieldErrors.departmentId)}
                >
                  <option value="">Выберите отделение</option>
                  {departments.map((department) => (
                    <option key={department.id} value={department.id}>
                      {department.name}
                    </option>
                  ))}
                </Form.Select>
                <FieldError message={fieldErrors.departmentId} />
              </Form.Group>

              <Form.Group>
                <Form.Label>Регион отделения</Form.Label>
                <Form.Control
                  value={selectedDepartment?.region || form.region}
                  readOnly
                  placeholder="Регион определится автоматически"
                />
              </Form.Group>
              <Form.Group>
                <Form.Label>Тип автомобиля</Form.Label>
                <Form.Select
                  name="vehicleType"
                  value={form.vehicleType}
                  onChange={handleFieldChange}
                  isInvalid={Boolean(fieldErrors.vehicleType)}
                >
                  <option value="">Выберите тип автомобиля</option>
                  <option value="STANDARD">Автомобиль</option>
                  <option value="ELECTRIC">Электромобиль</option>
                </Form.Select>
                <Form.Text className="helper-text">
                  Для обычного автомобиля подбираются номера 3256 XX-2, для электромобиля - E000 AA-7.
                </Form.Text>
                <FieldError message={fieldErrors.vehicleType} />
              </Form.Group>
            </div>

            <div className="detail-grid">
              <Form.Group>
                <Form.Label>VIN</Form.Label>
                <Form.Control
                  name="vehicleVin"
                  value={form.vehicleVin}
                  onChange={handleFieldChange}
                  maxLength={17}
                  placeholder="Например: WDB1240661C12345"
                  isInvalid={Boolean(fieldErrors.vehicleVin)}
                />
                <Form.Text className="helper-text">17 латинских букв и цифр</Form.Text>
                <FieldError message={fieldErrors.vehicleVin} />
              </Form.Group>

              <Form.Group>
                <Form.Label>Модель автомобиля</Form.Label>
                <Form.Control
                  name="vehicleModel"
                  value={form.vehicleModel}
                  onChange={handleFieldChange}
                  placeholder="Например: Volkswagen Polo"
                  isInvalid={Boolean(fieldErrors.vehicleModel)}
                />
                <FieldError message={fieldErrors.vehicleModel} />
              </Form.Group>

              <Form.Group>
                <Form.Label>Год выпуска</Form.Label>
                <Form.Control
                  name="vehicleYear"
                  value={form.vehicleYear}
                  onChange={handleFieldChange}
                  placeholder="Например: 2020"
                  isInvalid={Boolean(fieldErrors.vehicleYear)}
                />
                <FieldError message={fieldErrors.vehicleYear} />
              </Form.Group>

              <Form.Group>
                <Form.Label>Комментарий</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={4}
                  name="notes"
                  value={form.notes}
                  onChange={handleFieldChange}
                  placeholder="Например: прошу оформить выдачу в первой половине дня"
                  isInvalid={Boolean(fieldErrors.notes)}
                />
                <Form.Text className="helper-text">{form.notes.length}/{NOTES_MAX_LENGTH}</Form.Text>
                <FieldError message={fieldErrors.notes} />
              </Form.Group>
            </div>
          </div>
        ) : null}

        {step === 2 ? (
          <div className="wizard-grid">
            <div className="wizard-grid">
              {services.length ? (
                services.map((service) => (
                  <label
                    key={service.id}
                    className={`service-check ${form.serviceIds.includes(service.id) ? 'active' : ''}`}
                  >
                    <input
                      type="checkbox"
                      checked={form.serviceIds.includes(service.id)}
                      onChange={() => toggleService(service.id)}
                    />
                    <div>
                      <h5>{service.name}</h5>
                      <p>{service.description || 'Без описания'}</p>
                      {typeof service.price === 'number' ? (
                        <span className="soft-badge">{service.price} BYN</span>
                      ) : null}
                    </div>
                  </label>
                ))
              ) : (
                <div className="empty-state compact-empty">
                  <i className="bi bi-stars" />
                  <p>Дополнительных услуг сейчас нет.</p>
                </div>
              )}
            </div>

            {selectionMode === 'random' ? (
              <div className="info-banner">
                <i className="bi bi-shuffle" />
                <div>
                  <strong>Номер будет назначен автоматически</strong>
                  <div>
                    Система выдаст случайный свободный номер из всех отделений того же региона, что
                    и выбранное отделение.
                  </div>
                </div>
              </div>
            ) : null}

            {selectionMode === 'personalized' ? (
              <div className="wizard-grid">
                <Form.Group>
                  <Form.Label>Желаемый номер</Form.Label>
                  <Form.Control
                    name="plateNumber"
                    value={form.plateNumber}
                    onChange={handleFieldChange}
                    maxLength={9}
                    placeholder="Например: 3256 XX-2 или E000 AA-7"
                    isInvalid={Boolean(fieldErrors.plateNumber)}
                  />
                  <Form.Text className="helper-text">
                    Формат: 3256 XX-2 для обычного авто или E000 AA-7 для электромобиля
                  </Form.Text>
                  <FieldError message={fieldErrors.plateNumber} />
                </Form.Group>

                <div className="info-banner">
                  <i className="bi bi-shield-check" />
                  <div>
                    <strong>Персонализированный знак проверяется по всему региону</strong>
                    <div>
                      Если номер уже существует и свободен в этом регионе, система использует его.
                      Если номера нет в базе, он будет создан для выбранного отделения.
                    </div>
                    {availableRegionCodes.length ? (
                      <div>Допустимый код региона для отделения: {availableRegionCodes.join(' или ')}</div>
                    ) : null}
                  </div>
                </div>
              </div>
            ) : null}

            {selectionMode === 'existing' ? (
              <div className="wizard-grid">
                <Form.Group>
                  <Form.Label>Текущий номерной знак</Form.Label>
                  <Form.Control
                    name="plateNumber"
                    value={form.plateNumber}
                    onChange={handleFieldChange}
                    maxLength={9}
                    placeholder="Например: 3256 XX-2 или E000 AA-7"
                    isInvalid={Boolean(fieldErrors.plateNumber)}
                  />
                  <FieldError message={fieldErrors.plateNumber} />
                </Form.Group>

                <div className="info-banner">
                  <i className="bi bi-shield-check" />
                  <div>
                    <strong>Проверка существующего номера</strong>
                    <div>
                      Для услуги дубликатов номер должен существовать в базе и быть ранее выдан.
                      Для услуги сохранения номер должен существовать в базе.
                    </div>
                  </div>
                </div>
              </div>
            ) : null}

            {selectionMode === 'available' ? (
              <div className="wizard-grid">
                <div className="info-banner">
                  <i className="bi bi-grid-3x3-gap" />
                  <div>
                    <strong>Выберите один из доступных номеров</strong>
                    <div>
                      Показываем свободные номера из всех отделений того же региона, что и выбранное
                      отделение.
                    </div>
                  </div>
                </div>

                {fieldErrors.plateNumber ? <FieldError message={fieldErrors.plateNumber} /> : null}

                <div className="selection-grid">
                  {loadingPlates ? (
                    <div className="loading-state compact-loading">
                      <Spinner />
                    </div>
                  ) : filteredAvailablePlates.length ? (
                    filteredAvailablePlates.map((plate) => (
                      <button
                        type="button"
                        key={plate.id}
                        className={`selection-card ${form.plateNumber === getPlateView(plate) ? 'active' : ''}`}
                        onClick={() => {
                          setForm((current) => ({ ...current, plateNumber: getPlateView(plate) }));
                          setFieldErrors((current) => ({ ...current, plateNumber: '' }));
                        }}
                      >
                        <h4>{getPlateView(plate)}</h4>
                        <p>{plate.departmentName || 'Без отделения'}</p>
                        <div className="selection-meta">
                          <span className="soft-badge">{plate.region || 'Без региона'}</span>
                        </div>
                      </button>
                    ))
                  ) : (
                    <div className="empty-state compact-empty">
                      <i className="bi bi-search" />
                      <p>В регионе выбранного отделения сейчас нет доступных номеров для выбранного типа автомобиля.</p>
                    </div>
                  )}
                </div>
              </div>
            ) : null}
          </div>
        ) : null}

        {step === 3 ? (
          <div className="summary-list">
            <div className="summary-item">
              <span>Паспорт</span>
              <strong>{passportNumber || 'Не найден'}</strong>
            </div>
            <div className="summary-item">
              <span>Отделение</span>
              <strong>{selectedDepartment?.name || 'Не выбрано'}</strong>
            </div>
            <div className="summary-item">
              <span>Тип автомобиля</span>
              <strong>{form.vehicleType === 'ELECTRIC' ? 'Электромобиль' : 'Автомобиль'}</strong>
            </div>
            <div className="summary-item">
              <span>Способ получения номера</span>
              <strong>
                {selectionMode === 'random'
                  ? 'Случайный из доступных'
                  : selectionMode === 'personalized'
                    ? 'Свой персонализированный'
                    : 'Выбор из доступных'}
              </strong>
            </div>
            <div className="summary-item">
              <span>Номер</span>
              <strong>
                {selectionMode === 'random'
                  ? 'Будет назначен автоматически'
                  : selectedPlate?.plateNumber || form.plateNumber}
              </strong>
            </div>
            <div className="summary-item">
              <span>Услуги</span>
              <strong>
                {selectedServices.length ? selectedServices.map((item) => item.name).join(', ') : 'Без услуг'}
              </strong>
            </div>
            <div className="summary-item">
              <span>Автомобиль</span>
              <strong>{form.vehicleModel || 'Не указан'}</strong>
            </div>
            <div className="summary-item">
              <span>Комментарий</span>
              <strong>{form.notes || 'Нет комментария'}</strong>
            </div>
          </div>
        ) : null}

        {error ? (
          <Alert variant="danger" className="glass-alert mt-3">
            {error}
          </Alert>
        ) : null}
      </Modal.Body>
      <Modal.Footer className="modal-dark-footer wizard-footer">
        <Button variant="outline-light" className="glass-button" onClick={step === 1 ? onHide : goBack}>
          {step === 1 ? 'Отмена' : 'Назад'}
        </Button>
        {step < 3 ? (
          <Button className="primary-button" onClick={goNext}>
            Далее
          </Button>
        ) : (
          <Button className="primary-button" onClick={handleSubmit} disabled={loading}>
            {loading ? 'Создаем...' : 'Подтвердить заявление'}
          </Button>
        )}
      </Modal.Footer>
    </Modal>
  );
}

export function ApplicationDetailsModal({ show, onHide, application }) {
  const services = application?.additionalServices || [];
  const [allServices, setAllServices] = useState([]);
  const [selectedServiceName, setSelectedServiceName] = useState('');

  useEffect(() => {
    let cancelled = false;

    const loadServices = async () => {
      if (!show) {
        return;
      }

      try {
        const result = await servicesApi.getAll();
        if (!cancelled) {
          setAllServices(result);
        }
      } catch {
        if (!cancelled) {
          setAllServices([]);
        }
      }
    };

    loadServices();

    return () => {
      cancelled = true;
    };
  }, [show]);

  useEffect(() => {
    if (!show) {
      setSelectedServiceName('');
    }
  }, [show]);

  const selectedService = useMemo(
      () => allServices.find((service) => service.name === selectedServiceName),
      [allServices, selectedServiceName],
  );

  // Список полей для отображения в формате "поле — значение"
  const detailFields = useMemo(() => {
    if (!application) return [];

    return [
      { label: 'Номер заявления', value: `№${application.id}` },
      { label: 'Статус', value: getApplicationStatusLabel(application.status), isStatus: true },
      { label: 'Дата подачи', value: application.submissionDate || 'Не указана' },
      { label: 'Заявитель', value: application.applicantName || 'Не указан' },
      { label: 'Паспорт', value: application.applicantPassport || 'Не указан' },
      { label: 'Номерной знак', value: application.licensePlateNumber || 'Не выбран' },
      { label: 'Отделение', value: application.departmentName || 'Не указано' },
      { label: 'Автомобиль', value: application.vehicleModel || 'Не указан' },
      { label: 'VIN', value: application.vehicleVin || 'Не указан' },
      { label: 'Сумма', value: `${application.paymentAmount || 0} BYN` },
      { label: 'Комментарий заявителя', value: application.notes || 'Нет комментария' },
      { label: 'Комментарий администратора', value: application.adminComment || 'Комментарий отсутствует' },
    ];
  }, [application]);

  // Функция для получения класса статуса
  const getStatusClassForLabel = (status) => {
    const statusMap = {
      PENDING: 'pending',
      CONFIRMED: 'confirmed',
      COMPLETED: 'completed',
      CANCELLED: 'cancelled',
      EXPIRED: 'expired',
    };
    return statusMap[status] || 'default';
  };

  return (
      <Modal centered show={show} onHide={onHide} dialogClassName="glass-modal modal-xl application-details-modal">
        <Modal.Header closeButton closeVariant="white" className="modal-dark-header">
          <Modal.Title>Детали заявления</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {application ? (
              <>
                {/* Табличная форма "поле — значение" */}
                <div className="details-table">
                  {detailFields.map((field, index) => (
                      <div key={index} className="details-row">
                        <div className="details-label">{field.label}</div>
                        <div className="details-value">
                          {field.isStatus ? (
                              <span className={`status-pill ${getStatusClassForLabel(application.status)}`}>
                        {field.value}
                      </span>
                          ) : (
                              field.value
                          )}
                        </div>
                      </div>
                  ))}
                </div>

                {/* Блок услуг — оставляем с возможностью раскрытия описания */}
                <div className="services-section">
                  <div className="services-header">
                    <h3 className="section-title">Подключенные услуги</h3>
                    <span className="soft-badge">{services.length}</span>
                  </div>

                  {services.length ? (
                      <div className="services-list">
                        {services.map((service) => (
                            <div key={service} className="service-item-wrapper">
                              <button
                                  type="button"
                                  className={`service-item ${selectedServiceName === service ? 'active' : ''}`}
                                  onClick={() => setSelectedServiceName((current) => (current === service ? '' : service))}
                              >
                                <span className="service-name">{service}</span>
                                <span className="service-toggle">
                          <span className="soft-badge">
                            {selectedServiceName === service ? 'Свернуть' : 'Описание'}
                          </span>
                          <i className={`bi ${selectedServiceName === service ? 'bi-chevron-up' : 'bi-chevron-down'}`} />
                        </span>
                              </button>
                              <div className={`service-description ${selectedServiceName === service && selectedService?.name === service ? 'open' : ''}`}>
                                {selectedServiceName === service && selectedService?.name === service && (
                                    <div className="service-description-content">
                                      {selectedService.description || 'Описание пока не указано.'}
                                    </div>
                                )}
                              </div>
                            </div>
                        ))}
                      </div>
                  ) : (
                      <div className="empty-state compact-empty">
                        <i className="bi bi-stars" />
                        <p>Дополнительные услуги не выбраны.</p>
                      </div>
                  )}
                </div>
              </>
          ) : null}
        </Modal.Body>
      </Modal>
  );
}

export function ApplicationActionModal({
  show,
  onHide,
  onConfirm,
  actionType,
  application,
  loading = false,
}) {
  const [comment, setComment] = useState('');

  useEffect(() => {
    if (show) {
      setComment(application?.adminComment || '');
    }
  }, [application?.adminComment, show]);

  const actionConfig = {
    confirm: {
      title: 'Подтвердить заявление',
      text: 'После подтверждения заявление перейдет в статус «Подтверждено».',
      button: 'Подтвердить',
    },
    complete: {
      title: 'Завершить заявление',
      text: 'После завершения номерной знак будет считаться выданным.',
      button: 'Завершить',
    },
    cancel: {
      title: 'Отменить заявление',
      text: 'После отмены заявление перейдет в статус «Отменено».',
      button: 'Отменить заявление',
    },
  };

  const config = actionConfig[actionType] || actionConfig.confirm;

  return (
    <Modal centered show={show} onHide={onHide} dialogClassName="glass-modal">
      <Modal.Header closeButton closeVariant="white" className="modal-dark-header">
        <Modal.Title>{config.title}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <div className="detail-card">
          <div className="detail-label">Заявление</div>
          <div className="detail-value">
            #{application?.id} • {application?.licensePlateNumber || 'Без номера'}
          </div>
        </div>
        <p className="modal-description mt-3">{config.text}</p>
        <Form.Group className="mt-3">
          <Form.Label>Комментарий для заявителя</Form.Label>
          <Form.Control
            as="textarea"
            rows={3}
            value={comment}
            onChange={(event) => setComment(event.target.value.slice(0, 500))}
            placeholder="Например: заявление подтверждено, можно приехать в отделение с документами."
          />
        </Form.Group>
      </Modal.Body>
      <Modal.Footer className="modal-dark-footer">
        <Button variant="outline-light" className="glass-button" onClick={onHide}>
          Назад
        </Button>
        <Button className="primary-button" onClick={() => onConfirm(comment)} disabled={loading}>
          {loading ? 'Обрабатываем...' : config.button}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
