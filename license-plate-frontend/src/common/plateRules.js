const charMap = {
  1072: 'a',
  1073: 'b',
  1074: 'v',
  1075: 'g',
  1076: 'd',
  1077: 'e',
  1078: 'zh',
  1079: 'z',
  1080: 'i',
  1081: 'i',
  1082: 'k',
  1083: 'l',
  1084: 'm',
  1085: 'n',
  1086: 'o',
  1087: 'p',
  1088: 'r',
  1089: 's',
  1090: 't',
  1091: 'u',
  1092: 'f',
  1093: 'h',
  1094: 'c',
  1095: 'ch',
  1096: 'sh',
  1097: 'sch',
  1098: '',
  1099: 'y',
  1100: '',
  1101: 'e',
  1102: 'yu',
  1103: 'ya',
  1105: 'e',
};

export const transliterateToAscii = (value = '') =>
  Array.from(value.toLowerCase())
    .map((char) => charMap[char.charCodeAt(0)] ?? char)
    .join('');

export const normalizePlateServiceName = (value = '') => transliterateToAscii(value.trim());

export const isAvailablePlateServiceRule = (service) => {
  const name = normalizePlateServiceName(service?.name || '');
  return (
    (name.includes('dostup') && name.includes('nomer')) ||
    (name.includes('vybor') && name.includes('nomer')) ||
    name.includes('svobod') ||
    name.includes('5 dostup')
  );
};

export const isPersonalizedPlateServiceRule = (service) => {
  const name = normalizePlateServiceName(service?.name || '');
  return (
    name.includes('personaliz') ||
    name.includes('svoi nomer') ||
    name.includes('imennoi') ||
    name.includes('individual')
  );
};

export const isDuplicatePlateServiceRule = (service) => {
  const name = normalizePlateServiceName(service?.name || '');
  return name.includes('dublik') || name.includes('duplikat') || name.includes('duplicate');
};

export const isKeepPlateServiceRule = (service) => {
  const name = normalizePlateServiceName(service?.name || '');
  return (
    name.includes('sohran') ||
    name.includes('broniro') ||
    name.includes('hranen') ||
    name.includes('save') ||
    name.includes('current plate')
  );
};

export const resolveAllowedRegionCodesRule = (region = '') => {
  const value = transliterateToAscii(region.trim()).toUpperCase();
  const codes = new Set();

  if (value.includes('BREST')) codes.add('1');
  if (value.includes('VITEB')) codes.add('2');
  if (value.includes('GOMEL')) codes.add('3');
  if (value.includes('GRODN')) codes.add('4');
  if (value.includes('MINSK') && (value.includes('OBL') || value.includes('OBLAST'))) codes.add('5');
  if (value.includes('MOGIL')) codes.add('6');
  if (value.includes('MINSK') && !codes.has('5')) {
    codes.add('7');
    codes.add('8');
  }
  if (value.includes('VOORUZH') || value.includes('ARMED') || value.includes('FORCES')) {
    codes.add('0');
  }

  return [...codes];
};
