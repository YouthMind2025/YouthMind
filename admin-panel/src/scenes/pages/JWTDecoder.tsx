import React, { useState } from 'react';
import { jwtDecode } from 'jwt-decode';
import './JWTDecoder.css';

interface JWTPayload {
  sub?: string;
  exp?: number;
  iat?: number;
  email?: string;
  [key: string]: any;
}

interface DecodedToken {
  header: any;
  payload: JWTPayload;
  signature: string;
}

const JWTDecoder: React.FC = () => {
  const [jwtInput, setJwtInput] = useState('');
  const [decodedToken, setDecodedToken] = useState<DecodedToken | null>(null);
  const [error, setError] = useState('');
  const [isExpired, setIsExpired] = useState(false);

  const formatTimestamp = (timestamp: number) => {
    try {
      return new Date(timestamp * 1000).toLocaleString();
    } catch {
      return 'Invalid timestamp';
    }
  };

  const isTokenExpired = (exp: number) => {
    return exp < Date.now() / 1000;
  };

  const decodeJWT = () => {
    setError('');
    setDecodedToken(null);
    setIsExpired(false);

    if (!jwtInput.trim()) {
      setError('Please enter a JWT token');
      return;
    }

    try {
      // Validate JWT format (should have 3 parts separated by dots)
      const parts = jwtInput.trim().split('.');
      if (parts.length !== 3) {
        setError('Invalid JWT format. JWT should have 3 parts separated by dots.');
        return;
      }

      // Decode the payload
      const payload = jwtDecode<JWTPayload>(jwtInput);
      
      // Decode the header manually
      const header = JSON.parse(atob(parts[0]));
      
      // Extract signature
      const signature = parts[2];

      // Check if token is expired
      if (payload.exp) {
        setIsExpired(isTokenExpired(payload.exp));
      }

      setDecodedToken({
        header,
        payload,
        signature
      });

    } catch (err: any) {
      setError(`Failed to decode JWT: ${err.message || 'Invalid token format'}`);
    }
  };

  const clearInput = () => {
    setJwtInput('');
    setDecodedToken(null);
    setError('');
    setIsExpired(false);
  };

  const formatJson = (obj: any) => {
    return JSON.stringify(obj, null, 2);
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <h1 className="page-title">JWT Decoder</h1>
        <p className="page-subtitle">Paste and decode JWT tokens to inspect their payload and claims</p>
      </div>
      
      <div className="page-content">
        <div className="jwt-decoder-container">
          {/* Input Section */}
          <div className="jwt-input-section">
            <label htmlFor="jwt-input" className="jwt-label">
              JWT Token Input
            </label>
            <textarea
              id="jwt-input"
              className="jwt-textarea"
              value={jwtInput}
              onChange={(e) => setJwtInput(e.target.value)}
              placeholder="Paste your JWT token here (e.g., eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...)"
              rows={4}
            />
            <div className="jwt-buttons">
              <button 
                onClick={decodeJWT}
                className="jwt-button jwt-button-primary"
                disabled={!jwtInput.trim()}
              >
                Decode JWT
              </button>
              <button 
                onClick={clearInput}
                className="jwt-button jwt-button-secondary"
              >
                Clear
              </button>
            </div>
          </div>

          {/* Error Display */}
          {error && (
            <div className="jwt-error">
              <strong>Error:</strong> {error}
            </div>
          )}

          {/* Results Section */}
          {decodedToken && (
            <div className="jwt-results">
              {/* Token Status */}
              <div className="jwt-status">
                <span className={`jwt-status-badge ${isExpired ? 'expired' : 'valid'}`}>
                  {isExpired ? 'Token Expired' : 'Token Valid'}
                </span>
              </div>

              {/* Key Claims Section */}
              <div className="jwt-section">
                <h3 className="jwt-section-title">Key Claims</h3>
                <div className="jwt-claims-grid">
                  <div className="jwt-claim">
                    <span className="jwt-claim-label">Subject (sub):</span>
                    <span className="jwt-claim-value">{decodedToken.payload.sub || 'N/A'}</span>
                  </div>
                  <div className="jwt-claim">
                    <span className="jwt-claim-label">Email:</span>
                    <span className="jwt-claim-value">{decodedToken.payload.email || 'N/A'}</span>
                  </div>
                  <div className="jwt-claim">
                    <span className="jwt-claim-label">Issued At (iat):</span>
                    <span className="jwt-claim-value">
                      {decodedToken.payload.iat 
                        ? `${formatTimestamp(decodedToken.payload.iat)} (${decodedToken.payload.iat})`
                        : 'N/A'
                      }
                    </span>
                  </div>
                  <div className="jwt-claim">
                    <span className="jwt-claim-label">Expires At (exp):</span>
                    <span className={`jwt-claim-value ${isExpired ? 'expired' : ''}`}>
                      {decodedToken.payload.exp 
                        ? `${formatTimestamp(decodedToken.payload.exp)} (${decodedToken.payload.exp})`
                        : 'N/A'
                      }
                    </span>
                  </div>
                </div>
              </div>

              {/* Header Section */}
              <div className="jwt-section">
                <h3 className="jwt-section-title">Header</h3>
                <pre className="jwt-json-display">{formatJson(decodedToken.header)}</pre>
              </div>

              {/* Payload Section */}
              <div className="jwt-section">
                <h3 className="jwt-section-title">Payload (All Claims)</h3>
                <pre className="jwt-json-display">{formatJson(decodedToken.payload)}</pre>
              </div>

              {/* Signature Section */}
              <div className="jwt-section">
                <h3 className="jwt-section-title">Signature</h3>
                <div className="jwt-signature">
                  <code className="jwt-signature-text">{decodedToken.signature}</code>
                  <small className="jwt-signature-note">
                    Note: Signature verification requires the secret key
                  </small>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default JWTDecoder; 