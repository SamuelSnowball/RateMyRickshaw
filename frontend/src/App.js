import React, { useState } from 'react';
import './App.css';

// Replace this with your actual API Gateway endpoint after deployment
const API_ENDPOINT = process.env.REACT_APP_API_ENDPOINT || 'http://localhost:8081/analyze';

function App() {
  const [activeTab, setActiveTab] = useState('analyze'); // 'analyze', 'good-examples', 'bad-examples'
  const [imageUrl, setImageUrl] = useState('');
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [inputMode, setInputMode] = useState('url'); // 'url' or 'upload'
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [modalImage, setModalImage] = useState(null);

  const goodExamples = [
    {
      url: 'https://ratemyrickshaw.snowballsjourney.com/examples/good_rickshaw.jpg',
      description: 'Clear rear view of rickshaw'
    },
    {
      url: 'https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEhnTSoyGZeLUJSx884CWOrEKFRoCv6Sv4W0rfwYb03juMYEVpKs5daDoxOCXOB0_3hwill4fhpOSV90tlPp4AQ3GsCfjebN-rLV8IS_tfYOGxmZET7sytR2srHKaC2ozQLIB4UOoGPAlInT/s1600/120721+Phoenix+Mills+025.JPG',
      description: 'Side view with visible number plate'
    },
    {
      url: 'https://5.imimg.com/data5/ECOM/Default/2022/3/MK/FL/RS/24932764/61tlzwflyil-ac-sl1500-500x500.jpg',
      description: 'Front view with clear number plate'
    },
    {
      url: 'https://media.istockphoto.com/id/1387219905/vector/indian-auto-rickshaw-vector-illustration.jpg?s=612x612&w=0&k=20&c=kGbzd1SdAtH6zjsPKwIzwMH_-TB-me97BvKhQskbdEk=',
      description: 'Vector illustration of auto rickshaw'
    }
  ];

  const badExamples = [
    {
      url: '/examples/bad_0.jpg',
      description: 'Poor lighting or angle prevents detection',
      notes: 'This image just has too much going on, there are detections all over the place, no real chance to detect a number plate here. Interestingly Rekognition managed to detect "adidas" somewhere within this mess!'
    },
    {
      url: '/examples/bad_1.jpg',
      description: 'Obstructed or unclear number plate',
      notes: 'Only half of this number plate gets detected which is surprising, so no chance to construct the full plate number from it automatically.'

    }
  ];

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

  // Handle escape key to close modal
  React.useEffect(() => {
    const handleEscape = (e) => {
      if (e.key === 'Escape' && modalImage) {
        setModalImage(null);
      }
    };
    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [modalImage]);

  const handleTabChange = (tab) => {
    // Clear image preview and results when leaving analyze tab
    if (activeTab === 'analyze' && tab !== 'analyze') {
      setImagePreview(null);
      setResult(null);
      setError(null);
    }
    setActiveTab(tab);
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

      <div className="tabs">
        <button
          className={`tab ${activeTab === 'analyze' ? 'active' : ''}`}
          onClick={() => handleTabChange('analyze')}
        >
          🔍 Analyze
        </button>
        <button
          className={`tab ${activeTab === 'good-examples' ? 'active' : ''}`}
          onClick={() => handleTabChange('good-examples')}
        >
          ✅ Good Examples
        </button>
        <button
          className={`tab ${activeTab === 'bad-examples' ? 'active' : ''}`}
          onClick={() => handleTabChange('bad-examples')}
        >
          ❌ Bad Examples
        </button>
      </div>

      {activeTab === 'analyze' && (
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
                <div style={{ 
                  marginTop: '8px', 
                  padding: '8px 12px', 
                  backgroundColor: '#fff3cd', 
                  border: '1px solid #ffc107', 
                  borderRadius: '4px',
                  fontSize: '13px',
                  color: '#856404'
                }}>
                  ⚠️ <strong>Note:</strong> Image URL must use HTTPS. HTTP images will not render due to browser security restrictions.
                </div>
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
                <div className="detection-failed">
                  <div className="verdict failure">
                    ❌ Detection Failed:  {result.data || result.message || result.error || 'Analysis failed'}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {activeTab === 'good-examples' && (
        <div className="card examples-section">
          <h2>✅ Good Examples</h2>
          <p className="examples-description">
            These images demonstrate ideal conditions for number plate detection. Images containing multiple occurrences of the number plate, like the example on the right, should work.
          </p>
          <div className="examples-grid">
            {goodExamples.map((example, index) => (
              <div key={index} className="example-item">
                <img src={example.url} alt={example.description} />
                <p className="example-description">{example.description}</p>
                <button
                  className="try-example-btn"
                  onClick={() => {
                    setImageUrl(example.url);
                    setInputMode('url');
                    handleTabChange('analyze');
                  }}
                >
                  Try This Image
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'bad-examples' && (
        <div className="card examples-section">
          <h2>❌ Bad Examples</h2>
          <p className="examples-description">
            These images show challenging conditions where detection may fail:
            multiple vehicles, obstructions, or unclear plates.
          </p>
          <div className="examples-grid">
            {badExamples.map((example, index) => (
              <div key={index} className="example-item">
                <img 
                  src={example.url} 
                  alt={example.description}
                  onClick={() => setModalImage(example)}
                  style={{ cursor: 'pointer' }}
                />
                <p className="example-description">{example.description}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="footer">
        <p>Powered by AWS Rekognition & Quarkus Lambda</p>
      </div>

      {modalImage && (
        <div 
          className="modal-overlay" 
          onClick={() => setModalImage(null)}
        >
          <div className="modal-container" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Image Preview</h3>
              <button 
                className="modal-close" 
                onClick={() => setModalImage(null)}
                aria-label="Close"
              >
                ✕
              </button>
            </div>
            {modalImage.notes && (
              <div className="modal-notes">
                {modalImage.notes}
              </div>
            )}
            <div className="modal-body">
              <img 
                src={modalImage.url || modalImage} 
                alt="Full size view" 
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
