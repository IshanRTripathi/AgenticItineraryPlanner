import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const localesDir = path.join(__dirname, 'src/i18n/locales');
const languages = ['en', 'hi', 'es', 'kn', 'te', 'ml', 'pa', 'bn'];
const files = ['common.json', 'components.json', 'pages.json', 'errors.json', 'validation.json'];

function getKeys(obj, prefix = '') {
  let keys = [];
  for (const key in obj) {
    const fullKey = prefix ? `${prefix}.${key}` : key;
    if (typeof obj[key] === 'object' && obj[key] !== null && !Array.isArray(obj[key])) {
      keys = keys.concat(getKeys(obj[key], fullKey));
    } else {
      keys.push(fullKey);
    }
  }
  return keys.sort();
}

console.log('Validating translation structure...\n');

const enStructure = {};
files.forEach(file => {
  const filePath = path.join(localesDir, 'en', file);
  const content = JSON.parse(fs.readFileSync(filePath, 'utf8'));
  enStructure[file] = getKeys(content);
});

let allValid = true;

languages.slice(1).forEach(lang => {
  console.log(`Checking ${lang.toUpperCase()}...`);
  let langValid = true;
  
  files.forEach(file => {
    const filePath = path.join(localesDir, lang, file);
    if (!fs.existsSync(filePath)) {
      console.log(`  ❌ Missing: ${file}`);
      langValid = false;
      allValid = false;
      return;
    }
    
    const content = JSON.parse(fs.readFileSync(filePath, 'utf8'));
    const langKeys = getKeys(content);
    const enKeys = enStructure[file];
    
    const missing = enKeys.filter(k => !langKeys.includes(k));
    const extra = langKeys.filter(k => !enKeys.includes(k));
    
    if (missing.length > 0 || extra.length > 0) {
      console.log(`  ❌ ${file}:`);
      if (missing.length > 0) console.log(`     Missing keys: ${missing.join(', ')}`);
      if (extra.length > 0) console.log(`     Extra keys: ${extra.join(', ')}`);
      langValid = false;
      allValid = false;
    } else {
      console.log(`  ✅ ${file}`);
    }
  });
  
  if (langValid) console.log(`  ✅ All files valid\n`);
  else console.log();
});

if (allValid) {
  console.log('✅ All translations are correctly mapped!');
} else {
  console.log('❌ Some translations have issues.');
  process.exit(1);
}
