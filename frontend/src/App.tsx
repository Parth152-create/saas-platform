import { useEffect, useState } from 'react';

function App() {
  const [status, setStatus] = useState<string>('loading...');

  useEffect(() => {
    fetch('http://localhost:8081/api/ping')
      .then((res) => res.json())
      .then((data) => setStatus(data.status))
      .catch(() => setStatus('error — is the backend running?'));
  }, []);

  return (
    <div style={{ padding: '2rem', fontFamily: 'sans-serif' }}>
      <h1>SaaS Platform</h1>
      <p>Backend status: <strong>{status}</strong></p>
    </div>
  );
}

export default App;