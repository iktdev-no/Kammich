import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

// Gå ut av src/i18n for å nå public/locales
import noTranslations from './locales/no.json';
import enTranslations from './locales/en.json';

i18n
    .use(initReactI18next)
    .init({
        resources: {
            no: {
                translation: noTranslations,
            },
            nb: {
                translation: noTranslations,
            },
            en: {
                translation: enTranslations,
            },
        },
        lng: 'no',
        fallbackLng: 'en',
        interpolation: {
            escapeValue: false,
        },
    });

export default i18n;
