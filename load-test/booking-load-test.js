import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const baseUrl = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const testUsers = Number(__ENV.TEST_USERS || '20');
const eventCapacity = Number(__ENV.EVENT_CAPACITY || '10000');
const quantity = Number(__ENV.QUANTITY || '1');

const maxHttpFailureRate = __ENV.MAX_HTTP_FAILURE_RATE || 'rate<0.05';
const maxRequestDuration = __ENV.MAX_REQUEST_DURATION || 'p(95)<1000';
const password = __ENV.TEST_PASSWORD || 'LoadTest123!';

export const bookingSuccessCount = new Counter('booking_success_count');
export const bookingRejectionCount = new Counter('booking_rejection_count');
export const bookingUnexpectedFailureRate = new Rate('booking_unexpected_failure_rate');

export const options = {
  thresholds: {
    http_req_failed: [maxHttpFailureRate],
    http_req_duration: [maxRequestDuration],
    booking_unexpected_failure_rate: [maxHttpFailureRate],
  },
};

export function setup() {
  if (!Number.isInteger(quantity) || quantity < 1) {
    throw new Error('QUANTITY must be an integer greater than or equal to 1');
  }

  if (!Number.isInteger(testUsers) || testUsers < 1) {
    throw new Error('TEST_USERS must be an integer greater than or equal to 1');
  }

  if (!Number.isInteger(eventCapacity) || eventCapacity < 1) {
    throw new Error('EVENT_CAPACITY must be an integer greater than or equal to 1');
  }

  const runId = `${Date.now()}-${Math.floor(Math.random() * 1000000)}`;
  const adminEmail = `k6-admin-${runId}@example.com`;
  const admin = registerUser(adminEmail, `K6 Admin ${runId}`, 'ADMIN');
  const eventId = createEvent(admin.jwt, runId);
  const users = [];

  for (let index = 0; index < testUsers; index += 1) {
    const email = `k6-user-${runId}-${index}@example.com`;
    const user = registerUser(email, `K6 User ${index}`, 'USER');
    users.push({
      email,
      jwt: user.jwt,
    });
  }

  console.log(`Created load-test event ${eventId} with ${testUsers} test users.`);

  return {
    eventId,
    adminEmail,
    adminJwt: admin.jwt,
    userEmails: users.map((user) => user.email),
    tokens: users.map((user) => user.jwt),
  };
}

export default function (data) {
  const token = data.tokens[(__VU + __ITER) % data.tokens.length];
  const url = `${baseUrl}/events/${data.eventId}/bookings`;
  const payload = JSON.stringify({ quantity });

  const response = http.post(url, payload, {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    tags: {
      endpoint: 'create_booking',
    },
  });

  const created = response.status === 201;
  const rejected = response.status === 400 || response.status === 409;
  const expected = created || rejected;

  if (created) {
    bookingSuccessCount.add(1);
  } else if (rejected) {
    bookingRejectionCount.add(1);
  }

  bookingUnexpectedFailureRate.add(!expected);

  check(response, {
    'booking request returned expected status': () => expected,
    'successful booking returned 201': () => !created || response.status === 201,
  });

  sleep(Number(__ENV.SLEEP_SECONDS || '0'));
}

export function teardown(data) {
  console.log('');
  console.log('=== Manual cleanup information ===');
  console.log(`Event ID: ${data.eventId}`);
  console.log(`Admin email: ${data.adminEmail}`);
  console.log(`Admin JWT: ${data.adminJwt}`);
  console.log(`User emails: ${data.userEmails.join(', ')}`);
  console.log('');
  console.log('Cleanup steps:');
  console.log(`1. Delete the event with DELETE ${baseUrl}/events/${data.eventId} using the admin JWT above.`);
  console.log('2. If you need to remove test users, delete rows for the listed emails from the users table.');
  console.log('3. If you need to remove test bookings, delete rows from bookings where event_id matches the event ID above.');
  console.log('4. If using the outbox/Redis notification pipeline during tests, clear related test outbox rows and Redis queues if desired.');
  console.log('==================================');
}

function registerUser(email, name, role) {
  const response = http.post(
    `${baseUrl}/auth/register`,
    JSON.stringify({
      email,
      name,
      password,
      role,
    }),
    {
      headers: {
        'Content-Type': 'application/json',
      },
      tags: {
        endpoint: 'register_test_user',
      },
    },
  );

  if (response.status !== 201) {
    throw new Error(`Failed to register ${role} ${email}: ${response.status} ${response.body}`);
  }

  const body = response.json();
  if (!body.jwt) {
    throw new Error(`Registration response for ${email} did not include jwt`);
  }

  return body;
}

function createEvent(adminToken, runId) {
  const startTime = new Date(Date.now() + 60 * 60 * 1000);
  const endTime = new Date(Date.now() + 2 * 60 * 60 * 1000);
  const response = http.post(
    `${baseUrl}/events`,
    JSON.stringify({
      name: `K6 Booking Load Test ${runId}`,
      description: 'Temporary event created by k6 booking load test.',
      venue: 'Load Test Venue',
      startTime: startTime.toISOString(),
      endTime: endTime.toISOString(),
      capacity: eventCapacity,
    }),
    {
      headers: {
        Authorization: `Bearer ${adminToken}`,
        'Content-Type': 'application/json',
      },
      tags: {
        endpoint: 'create_test_event',
      },
    },
  );

  if (response.status !== 201) {
    throw new Error(`Failed to create event: ${response.status} ${response.body}`);
  }

  const body = response.json();
  if (!body.data || !body.data.id) {
    throw new Error('Create event response did not include data.id');
  }

  return body.data.id;
}
