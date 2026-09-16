import 'i18next';
import noTranslations from './locales/no.json';

declare module 'i18next' {
    interface CustomTypeOptions {
        resources: {
            translation: typeof noTranslations;
        };
    }
}
