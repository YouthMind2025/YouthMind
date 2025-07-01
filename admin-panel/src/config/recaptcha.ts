export const RECAPTCHA_CONFIG = {
  siteKey: process.env.REACT_APP_RECAPTCHA_SITE_KEY || '6Ld_JHQrAAAAAL7oSCR49yxsk51Jxnzqoq_0qKlq',
  // Use invisible reCAPTCHA for better UX
  invisible: true,
  // Optional: Hide the badge
  badge: 'bottomright' as const
}; 