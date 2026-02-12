import React, { useState } from 'react';
import './App.css';

// Replace this with your actual API Gateway endpoint after deployment
const API_ENDPOINT = process.env.REACT_APP_API_ENDPOINT || 'YOUR_API_GATEWAY_ENDPOINT_HERE';

function App() {
  const [imageUrl, setImageUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const analyzeImage = async (e) => {
    e.preventDefault();
    
    if (!imageUrl.trim()) {
      setError('Please enter an image URL');
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await fetch(`${API_ENDPOINT}/analyze`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          imageUrl: imageUrl.trim()
        })
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      setResult(data);
    } catch (err) {
      setError(`Failed to analyze image: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="App">
      <div className="header">
        <h1>🛺 Rate My Rickshaw</h1>
        <p>AI-Powered Rickshaw Detection & Analysis</p>
      </div>

      <div className="card">
        {API_ENDPOINT === 'YOUR_API_GATEWAY_ENDPOINT_HERE' && (
          <div className="api-config">
            <strong>⚠️ Configuration Required:</strong> Please update the API_ENDPOINT in App.js or set REACT_APP_API_ENDPOINT environment variable with your API Gateway URL.
          </div>
        )}

        <form onSubmit={analyzeImage}>
          <div className="form-group">
            <label htmlFor="imageUrl">Image URL</label>
            <input
              id="imageUrl"
              type="url"
              placeholder="https://example.com/rickshaw.jpg"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              disabled={loading}
            />
          </div>

          <button type="submit" className="btn" disabled={loading}>
            {loading ? 'Analyzing' : 'Analyze Image'}
          </button>
        </form>

        {loading && <div className="loading">Analyzing your image</div>}

        {error && <div className="error">{error}</div>}

        {result && (
          <div className="result">
            {result.success ? (
              <>
                <div className={`verdict ${result.isRickshaw ? 'success' : 'failure'}`}>
                  {result.isRickshaw ? (
                    <>
                      ✅ Rickshaw Detected!
                      {result.rickshawConfidence > 0 && (
                        <div className="confidence">
                          Confidence: {result.rickshawConfidence.toFixed(2)}%
                        </div>
                      )}
                    </>
                  ) : (
                    <>
                      ❌ No Rickshaw Detected
                      <div className="confidence">
                        This doesn't appear to be a rickshaw
                      </div>
                    </>
                  )}
                </div>

                {result.labels && result.labels.length > 0 && (
                  <div className="labels">
                    <h3>Detected Objects:</h3>
                    <div className="label-list">
                      {result.labels.map((label, index) => (
                        <div key={index} className="label-item">
                          {label}
                          {result.labelConfidence && result.labelConfidence[label] && (
                            <span className="confidence-score">
                              {result.labelConfidence[label].toFixed(1)}%
                            </span>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </>
            ) : (
              <div className="error">
                {result.message || 'Analysis failed'}
              </div>
            )}
          </div>
        )}
      </div>

      <div className="footer">
        <p>Powered by AWS Rekognition & Quarkus Lambda</p>
      </div>
    </div>
  );
}

export default App;
