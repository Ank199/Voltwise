const {DatabaseSync}=require('node:sqlite');
const db=new DatabaseSync(process.argv[2] || 'verification/device-after.db');
console.log('schema',db.prepare('PRAGMA user_version').get());
console.log('sessions',db.prepare('SELECT sensorVersion, count(*) AS count FROM battery_sessions GROUP BY sensorVersion').all());
console.log('latest sessions',db.prepare('SELECT * FROM battery_sessions WHERE sensorVersion=2 ORDER BY id DESC LIMIT 3').all());
console.log('latest samples',db.prepare('SELECT * FROM battery_observations WHERE sessionId IN (SELECT id FROM battery_sessions WHERE sensorVersion=2) ORDER BY id DESC LIMIT 3').all());
console.log('events',db.prepare('SELECT * FROM timeline_events ORDER BY id DESC LIMIT 10').all());
db.close();
