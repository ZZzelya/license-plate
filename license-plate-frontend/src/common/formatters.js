export const formatBelarusPhone = (value = '') => {
  const digitsOnly = value.replace(/\D/g, '');

  let localDigits = digitsOnly;

  if (localDigits.startsWith('375')) {
    localDigits = localDigits.slice(3);
  } else if (localDigits.startsWith('80')) {
    localDigits = localDigits.slice(2);
  } else if (localDigits.startsWith('0')) {
    localDigits = localDigits.slice(1);
  }

  localDigits = localDigits.slice(0, 9);

  if (!localDigits) {
    return '';
  }

  const code = localDigits.slice(0, 2);
  const first = localDigits.slice(2, 5);
  const second = localDigits.slice(5, 7);
  const third = localDigits.slice(7, 9);

  let formatted = '+375';

  if (code) {
    formatted += ` (${code}`;
  }
  if (code.length === 2) {
    formatted += ')';
  }
  if (first) {
    formatted += ` ${first}`;
  }
  if (second) {
    formatted += `-${second}`;
  }
  if (third) {
    formatted += `-${third}`;
  }

  return formatted;
};
