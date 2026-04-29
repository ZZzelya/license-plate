import { useEffect, useMemo, useState } from 'react';
import { Button, Col, Form, Modal, Row } from 'react-bootstrap';
import { resolveAllowedRegionCodesRule } from '../common/plateRules';

const initialForm = {
  plateType: 'standard',
  plateNumber: '',
  series: '',
  regionCode: '',
  departmentId: '',
};

const STANDARD_SERIES_REGEX = /^[ABEKMHOPCTX]{2}$/;
const ELECTRIC_SERIES_REGEX = /^[A-Z]{2}$/;

const parsePlate = (plateNumber = '') => {
  const normalized = plateNumber.toUpperCase();
  const standard = normalized.match(/^(\d{4})\s([ABEKMHOPCTX]{2})-(\d)$/);
  const electric = normalized.match(/^(E\d{3})\s([A-Z]{2})-(\d)$/);
  const match = standard || electric;

  if (!match) {
    return { plateType: 'standard', plateNumber: '', series: '', regionCode: '' };
  }

  return {
    plateType: match[1].startsWith('E') ? 'electric' : 'standard',
    plateNumber: match[1],
    series: match[2],
    regionCode: match[3],
  };
};

const formatNumberPart = (value, plateType) => {
  if (plateType === 'electric') {
    const digits = value.toUpperCase().replace(/^E/, '').replace(/\D/g, '').slice(0, 3);
    return digits ? `E${digits}` : 'E';
  }

  return value.replace(/\D/g, '').slice(0, 4);
};

export function LicensePlateFormModal({
  show,
  onHide,
  onSubmit,
  plate,
  departments = [],
  loading,
}) {
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState('');

  const selectedDepartment = useMemo(
    () => departments.find((department) => department.id === Number(form.departmentId)),
    [departments, form.departmentId],
  );

  const availableRegionCodes = useMemo(
    () => resolveAllowedRegionCodesRule(selectedDepartment?.region || ''),
    [selectedDepartment?.region],
  );

  useEffect(() => {
    if (!show) return;

    if (plate) {
      const parsed = parsePlate(plate.plateNumber);
      setForm({
        plateType: parsed.plateType,
        plateNumber: plate.numberPart || parsed.plateNumber,
        series: plate.series || parsed.series,
        regionCode: plate.regionCode || parsed.regionCode,
        departmentId: plate.departmentId || '',
      });
    } else {
      setForm(initialForm);
    }

    setError('');
  }, [plate, show]);

  useEffect(() => {
    if (!selectedDepartment || !availableRegionCodes.length) return;

    setForm((current) => ({
      ...current,
      regionCode: availableRegionCodes.includes(current.regionCode) ? current.regionCode : availableRegionCodes[0],
    }));
  }, [availableRegionCodes, selectedDepartment]);

  const validationMessage = useMemo(() => {
    if (form.plateType === 'electric') {
      if (!/^E\d{3}$/.test(form.plateNumber.trim())) {
        return 'Номер электромобиля должен быть в формате E000';
      }
      if (!ELECTRIC_SERIES_REGEX.test(form.series.trim().toUpperCase())) {
        return 'Серия электромобиля: 2 латинские буквы, например AA или AB';
      }
    } else {
      if (!/^\d{4}$/.test(form.plateNumber.trim())) {
        return 'Номер должен состоять из 4 цифр';
      }
      if (!STANDARD_SERIES_REGEX.test(form.series.trim().toUpperCase())) {
        return 'Серия: 2 допустимые латинские буквы A, B, E, K, M, H, O, P, C, T, X';
      }
    }

    if (!form.departmentId) return 'Выберите отделение';
    if (!form.regionCode) return 'Не удалось определить код региона для отделения';
    return '';
  }, [form.departmentId, form.plateNumber, form.plateType, form.regionCode, form.series]);

  const handleChange = ({ target }) => {
    let value = target.value;

    if (target.name === 'plateType') {
      setForm((current) => ({
        ...current,
        plateType: value,
        plateNumber: value === 'electric' ? 'E' : '',
        series: '',
      }));
      setError('');
      return;
    }

    if (target.name === 'plateNumber') {
      value = formatNumberPart(value, form.plateType);
    }

    if (target.name === 'series') {
      const allowedPattern = form.plateType === 'electric' ? /[^A-Z]/g : /[^ABEKMHOPCTX]/g;
      value = value.toUpperCase().replace(allowedPattern, '').slice(0, 2);
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

    onSubmit({
      plateNumber: form.plateNumber.toUpperCase(),
      series: form.series.toUpperCase(),
      regionCode: form.regionCode,
      departmentId: Number(form.departmentId),
    });
  };

  const fullPlateNumber =
    form.plateNumber && form.series && form.regionCode
      ? `${form.plateNumber} ${form.series}-${form.regionCode}`
      : '';

  return (
    <Modal centered show={show} onHide={onHide} dialogClassName="glass-modal">
      <Modal.Header closeButton closeVariant="white" className="modal-dark-header">
        <Modal.Title>{plate ? 'Редактирование номерного знака' : 'Новый номерной знак'}</Modal.Title>
      </Modal.Header>
      <Form onSubmit={handleSubmit}>
        <Modal.Body>
          <Row className="g-3">
            <Col md={12}>
              <Form.Group>
                <Form.Label>Тип номера</Form.Label>
                <Form.Select name="plateType" value={form.plateType} onChange={handleChange}>
                  <option value="standard">Обычный автомобиль</option>
                  <option value="electric">Электромобиль</option>
                </Form.Select>
              </Form.Group>
            </Col>

            <Col md={6}>
              <Form.Group>
                <Form.Label>Номер</Form.Label>
                <Form.Control
                  name="plateNumber"
                  value={form.plateNumber}
                  onChange={handleChange}
                  placeholder={form.plateType === 'electric' ? 'Например: E000' : 'Например: 3256'}
                />
              </Form.Group>
            </Col>

            <Col md={6}>
              <Form.Group>
                <Form.Label>Серия</Form.Label>
                <Form.Control
                  name="series"
                  value={form.series}
                  onChange={handleChange}
                  placeholder={form.plateType === 'electric' ? 'Например: AA' : 'Например: AB'}
                />
              </Form.Group>
            </Col>

            <Col md={12}>
              <Form.Group>
                <Form.Label>Отделение</Form.Label>
                <Form.Select name="departmentId" value={form.departmentId} onChange={handleChange}>
                  <option value="">Выберите отделение</option>
                  {departments.map((department) => (
                    <option key={department.id} value={department.id}>
                      {department.name} • {department.region}
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>
            </Col>

            <Col md={6}>
              <Form.Group>
                <Form.Label>Код региона</Form.Label>
                <Form.Select
                  name="regionCode"
                  value={form.regionCode}
                  onChange={handleChange}
                  disabled={!availableRegionCodes.length}
                >
                  <option value="">Выберите код</option>
                  {availableRegionCodes.map((code) => (
                    <option key={code} value={code}>
                      {code}
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>
            </Col>

            <Col md={6}>
              <Form.Group>
                <Form.Label>Полный номер</Form.Label>
                <Form.Control value={fullPlateNumber} readOnly placeholder="Соберется автоматически" />
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
            {loading ? 'Сохраняем...' : plate ? 'Сохранить' : 'Создать'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}

export function LicensePlateViewModal({ show, onHide, plate }) {
  return (
    <Modal centered show={show} onHide={onHide} dialogClassName="glass-modal">
      <Modal.Header closeButton closeVariant="white" className="modal-dark-header">
        <Modal.Title>Информация о номерном знаке</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {plate ? (
          <div className="detail-grid">
            <div className="detail-card">
              <div className="detail-label">Полный номер</div>
              <div className="detail-value">{plate.plateNumber}</div>
            </div>
            <div className="detail-card">
              <div className="detail-label">Номер</div>
              <div className="detail-value">{plate.numberPart || 'Не указан'}</div>
            </div>
            <div className="detail-card">
              <div className="detail-label">Серия</div>
              <div className="detail-value">{plate.series || 'Не указана'}</div>
            </div>
            <div className="detail-card">
              <div className="detail-label">Код региона</div>
              <div className="detail-value">{plate.regionCode || 'Не указан'}</div>
            </div>
            <div className="detail-card">
              <div className="detail-label">Отделение</div>
              <div className="detail-value">{plate.departmentName || 'Не указано'}</div>
            </div>
            <div className="detail-card">
              <div className="detail-label">Регион</div>
              <div className="detail-value">{plate.region || 'Не указан'}</div>
            </div>
            <div className="detail-card">
              <div className="detail-label">Дата выдачи</div>
              <div className="detail-value">{plate.issueDate || 'Не выдавался'}</div>
            </div>
            <div className="detail-card">
              <div className="detail-label">Срок действия</div>
              <div className="detail-value">{plate.expiryDate || 'Не указан'}</div>
            </div>
          </div>
        ) : null}
      </Modal.Body>
    </Modal>
  );
}
