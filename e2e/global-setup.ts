import { GenericContainer, Wait, type StartedTestContainer } from 'testcontainers';
import { execSync, spawn, type ChildProcess } from 'child_process';

declare global {
	var __pgContainer: StartedTestContainer | undefined;
	var __backendProcess: ChildProcess | undefined;
	var __frontendProcess: ChildProcess | undefined;
}

export default async function globalSetup() {
	console.log('Starting PostgreSQL via Testcontainers...');
	const pgContainer = await new GenericContainer('postgres:17-alpine')
		.withEnvironment({
			POSTGRES_DB: 'meetdb',
			POSTGRES_USER: 'postgres',
			POSTGRES_PASSWORD: 'postgres',
		})
		.withExposedPorts(5432)
		.withWaitStrategy(Wait.forLogMessage(/ready to accept connections/, 2))
		.start();

	const pgPort = pgContainer.getMappedPort(5432);
	const pgHost = pgContainer.getHost();
	const dbUrl = `jdbc:postgresql://${pgHost}:${pgPort}/meetdb`;

	console.log(`PostgreSQL running at ${pgHost}:${pgPort}`);

	// Store container reference for teardown
	globalThis.__pgContainer = pgContainer;

	// Build backend if needed
	console.log('Building backend...');
	execSync('./gradlew build -x test', { cwd: '../backend', stdio: 'inherit' });

	// Start backend
	console.log('Starting backend...');
	const backendProcess = spawn(
		'java',
		[
			`-Dquarkus.datasource.jdbc.url=${dbUrl}`,
			'-Dquarkus.datasource.username=postgres',
			'-Dquarkus.datasource.password=postgres',
			'-Dquarkus.datasource.devservices.enabled=false',
			'-Dquarkus.flyway.migrate-at-start=true',
			'-jar',
			'../backend/build/quarkus-app/quarkus-run.jar',
		],
		{ stdio: 'pipe' },
	);

	globalThis.__backendProcess = backendProcess;

	// Wait for backend to be ready
	await waitForUrl('http://localhost:8080/api/events', 30000);
	console.log('Backend ready');

	// Build and start frontend preview
	console.log('Building frontend...');
	execSync('npm run build', { cwd: '../frontend', stdio: 'inherit' });

	const frontendProcess = spawn('npx', ['vite', 'preview', '--port', '4173'], {
		cwd: '../frontend',
		stdio: 'pipe',
	});

	globalThis.__frontendProcess = frontendProcess;

	await waitForUrl('http://localhost:4173', 10000);
	console.log('Frontend ready');
}

async function waitForUrl(url: string, timeout: number): Promise<void> {
	const start = Date.now();
	while (Date.now() - start < timeout) {
		try {
			const res = await fetch(url);
			if (res.ok || res.status < 500) return;
		} catch {
			// Server not ready yet, retry
		}
		await new Promise((r) => setTimeout(r, 500));
	}
	throw new Error(`Timed out waiting for ${url}`);
}
