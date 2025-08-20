export const RECAPTCHA_CONFIG = {
  // Use Google provided test key for local development to avoid Timeout (B) errors
  siteKey: process.env.REACT_APP_RECAPTCHA_SITE_KEY || '6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI',
  // Use invisible reCAPTCHA for better UX
  invisible: true,
  // Optional: Hide the badge
  badge: 'bottomright' as const
}; 