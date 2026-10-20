exports.checkDateAvailability = (date) => {
  if (!date) return false;
  return new Date(date) >= new Date();
};

exports.runEveryMidnight = () => {
  // Retained for backward compatibility
  console.log('[CRON] Midnight maintenance task initialized');
};
