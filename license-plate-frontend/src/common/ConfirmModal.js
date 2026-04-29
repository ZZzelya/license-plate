import { Button, Modal } from 'react-bootstrap';

function ConfirmModal({
  show,
  onHide,
  onConfirm,
  title = 'Подтвердите действие',
  message = 'Вы уверены, что хотите продолжить?',
  confirmText = 'Подтвердить',
  cancelText = 'Отмена',
  variant = 'danger',
  loading = false,
}) {
  return (
    <Modal centered show={show} onHide={onHide} dialogClassName="glass-modal">
      <Modal.Header closeButton closeVariant="white" className="modal-dark-header">
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <div className="confirm-modal-icon">
          <i className="bi bi-shield-exclamation" />
        </div>
        <p className="modal-description">{message}</p>
      </Modal.Body>
      <Modal.Footer className="modal-dark-footer">
        <Button variant="outline-light" className="glass-button" onClick={onHide} disabled={loading}>
          {cancelText}
        </Button>
        <Button variant={variant} className="primary-button" onClick={onConfirm} disabled={loading}>
          {loading ? 'Сохраняем...' : confirmText}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

export default ConfirmModal;
