import React, { useState } from 'react';
import './App.css';

// Replace this with your actual API Gateway endpoint after deployment
const API_ENDPOINT = process.env.REACT_APP_API_ENDPOINT || 'http://localhost:8081/analyze';

function App() {
  const [imageUrl, setImageUrl] = useState('');
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [inputMode, setInputMode] = useState('url'); // 'url' or 'upload'
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      // Validate file type
      if (!file.type.startsWith('image/')) {
        setError('Please select an image file');
        return;
      }
      // Validate file size (max 10MB)
      if (file.size > 10 * 1024 * 1024) {
        setError('Image size must be less than 10MB');
        return;
      }
      setImageFile(file);
      setError(null);
      
      // Create preview
      const reader = new FileReader();
      reader.onloadend = () => {
        setImagePreview(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const fileToBase64 = (file) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result);
      reader.onerror = (error) => reject(error);
    });
  };

  const analyzeImage = async (e) => {
    e.preventDefault();
    
    if (inputMode === 'url' && !imageUrl.trim()) {
      setError('Please enter an image URL');
      return;
    }

    if (inputMode === 'upload' && !imageFile) {
      setError('Please select an image file');
      return;
    }

    // Set preview for URL mode
    if (inputMode === 'url') {
      setImagePreview(imageUrl.trim());
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      let requestBody;
      
      if (inputMode === 'url') {
        requestBody = {
          imageUrl: imageUrl.trim()
        };
      } else {
        const base64Image = await fileToBase64(imageFile);
        requestBody = {
          imageBase64: base64Image
        };
      }

      const response = await fetch(`${API_ENDPOINT}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody)
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
          <div className="input-mode-selector">
            <button
              type="button"
              className={`mode-btn ${inputMode === 'url' ? 'active' : ''}`}
              onClick={() => {
                setInputMode('url');
                setImageFile(null);
                setImagePreview(null);
                setError(null);
              }}
            >
              🔗 Image URL
            </button>
            <button
              type="button"
              className={`mode-btn ${inputMode === 'upload' ? 'active' : ''}`}
              onClick={() => {
                setInputMode('upload');
                setImageUrl('');
                setImagePreview(null);
                setError(null);
              }}
            >
              📁 Upload File
            </button>
          </div>

          {inputMode === 'url' ? (
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
          ) : (
            <div className="form-group">
              <label htmlFor="imageFile">Select Image File</label>
              <input
                id="imageFile"
                type="file"
                accept="image/*"
                onChange={handleFileChange}
                disabled={loading}
              />
              {imageFile && (
                <div className="file-info">
                  ✓ {imageFile.name} ({(imageFile.size / 1024).toFixed(0)} KB)
                </div>
              )}
            </div>
          )}

          <button type="submit" className="btn" disabled={loading}>
            {loading ? 'Analyzing' : 'Analyze Image'}
          </button>
        </form>

        {imagePreview && (
          <div className="image-preview">
            <h3>Image Preview</h3>
            <img src={imagePreview} alt="Preview" />
          </div>
        )}

        {loading && <div className="loading">Analyzing your image</div>}

        {error && <div className="error">{error}</div>}

        {result && (
          <div className="result">
            {result.success ? (
              <>
                <div className="verdict success">
                  ✅ Number Plate Detected!
                </div>
                {result.data && (
                  <div className="number-plate">
                    <h3>Detected Number Plate:</h3>
                    <div className="plate-value">{result.data}</div>
                  </div>
                )}
                {result.error && (
                  <div className="info-message">
                    {result.error}
                  </div>
                )}
              </>
            ) : (
              <div className="error">
                {result.data || result.error || 'Analysis failed'}
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
