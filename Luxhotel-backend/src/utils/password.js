const crypto = require('crypto');

// Giống hệt: MessageDigest.getInstance("SHA-256") trong DatabaseHelper.kt
function hashPassword(password) {
  return crypto.createHash('sha256').update(password, 'utf8').digest('hex');
}

module.exports = { hashPassword };
