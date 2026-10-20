const path = require("path");
const fs = require("fs");
const multer = require("multer");

const busDir = path.join(__dirname, '..', 'public', 'uploads', 'busimage');
if (!fs.existsSync(busDir)) {
  fs.mkdirSync(busDir, { recursive: true });
}

const ownerDir = path.join(__dirname, '..', 'public', 'uploads', 'ownerAvatar');
if (!fs.existsSync(ownerDir)) {
  fs.mkdirSync(ownerDir, { recursive: true });
}

const busImageStorage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, busDir);
  },
  filename: function (req, file, cb) {
    const ext = path.extname(file.originalname || '') || '.jpg';
    cb(null, 'bus-' + Date.now() + '-' + Math.round(Math.random() * 1E9) + ext);
  }
});

const ownerAvatarStorage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, ownerDir);
  },
  filename: function (req, file, cb) {
    const ext = path.extname(file.originalname || '') || '.jpg';
    cb(null, 'avatar-' + Date.now() + ext);
  }
});

const fileFilter = (req, file, cb) => {
  if (
    !file.mimetype ||
    file.mimetype.startsWith('image/') ||
    file.mimetype === 'application/octet-stream' ||
    /\.(jpe?g|png|webp|gif|bmp|heic|heif)$/i.test(file.originalname || '')
  ) {
    return cb(null, true);
  }
  cb(new Error('Only image files are allowed'));
};

exports.uploadBusImage = multer({
  storage: busImageStorage,
  limits: { fileSize: 10 * 1024 * 1024 },
  fileFilter
}).single("image");

exports.uploadOwnerAvatar = multer({
  storage: ownerAvatarStorage,
  limits: { fileSize: 10 * 1024 * 1024 },
  fileFilter
}).single("photo");