const PAYMENT_METHODS = [
  'CREDIT_CARD',
  'DEBIT_CARD',
  'PIX',
  'BANK_TRANSFER',
  'BOLETO'
];

const REJECTION_REASONS = [
  'INSUFFICIENT_FUNDS',
  'CARD_EXPIRED',
  'FRAUD_SUSPECTED',
  'BANK_TIMEOUT',
  'INVALID_ACCOUNT'
];

function randomElement(array) {
  return array[Math.floor(Math.random() * array.length)];
}

function generateBankReference() {
  const hex = Math.random().toString(16).substring(2, 8).toUpperCase();
  return `BNK-${hex}`;
}

let paymentCounter = 0;

function generatePaymentId() {
  paymentCounter++;
  const date = new Date().toISOString().slice(0, 10).replace(/-/g, '');
  const seq = String(paymentCounter).padStart(5, '0');
  return `PAY-${date}-${seq}`;
}

function generatePaymentConfirmation(saleId, amount) {
  const rand = Math.random();
  let status;
  let rejectionReason = null;

  if (rand < 0.85) {
    status = 'CONFIRMED';
  } else if (rand < 0.95) {
    status = 'REJECTED';
    rejectionReason = randomElement(REJECTION_REASONS);
  } else {
    status = 'PENDING';
  }

  return {
    paymentId: generatePaymentId(),
    saleId: String(saleId),
    status,
    amount: parseFloat(amount).toFixed(2),
    currency: 'BRL',
    paymentMethod: randomElement(PAYMENT_METHODS),
    bankReference: generateBankReference(),
    confirmationDate: new Date().toISOString(),
    ...(rejectionReason && { rejectionReason })
  };
}

module.exports = { generatePaymentConfirmation };
