import React, { useState, useEffect } from 'react';
import { MappingRule, MappingRuleUpdateRequest } from '../../types';

interface MappingRuleEditModalProps {
  rule: MappingRule | null;
  isOpen: boolean;
  onClose: () => void;
  onSave: (ruleId: string, data: MappingRuleUpdateRequest) => Promise<void>;
  loading?: boolean;
}

const MappingRuleEditModal: React.FC<MappingRuleEditModalProps> = ({
  rule,
  isOpen,
  onClose,
  onSave,
  loading = false
}) => {
  const [formData, setFormData] = useState<MappingRuleUpdateRequest>({
    sadPercentage: 0,
    fearPercentage: 0,
    angryPercentage: 0,
    happyPercentage: 0,
    conflictedPercentage: 0,
    surprisedPercentage: 0
  });
  
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Initialize form data when rule changes
  useEffect(() => {
    if (rule) {
      setFormData({
        sadPercentage: rule.sadPercentage,
        fearPercentage: rule.fearPercentage,
        angryPercentage: rule.angryPercentage,
        happyPercentage: rule.happyPercentage,
        conflictedPercentage: rule.conflictedPercentage,
        surprisedPercentage: rule.surprisedPercentage
      });
      setErrors({});
    }
  }, [rule]);

  // Calculate total percentage
  const totalPercentage = Object.values(formData).reduce((sum, value) => sum + value, 0);
  
  // Check if form is valid
  const isValid = totalPercentage === 100 && Object.values(formData).every(value => value >= 0 && value <= 100);

  const handleInputChange = (field: keyof MappingRuleUpdateRequest, value: string) => {
    const numValue = parseInt(value) || 0;
    setFormData(prev => ({
      ...prev,
      [field]: numValue
    }));
    
    // Clear field error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({
        ...prev,
        [field]: ''
      }));
    }
  };

  const validateField = (field: keyof MappingRuleUpdateRequest, value: number) => {
    if (value < 0 || value > 100) {
      return `${field.replace('Percentage', '')} percentage must be between 0 and 100`;
    }
    return '';
  };

  const handleBlur = (field: keyof MappingRuleUpdateRequest) => {
    const value = formData[field];
    const error = validateField(field, value);
    if (error) {
      setErrors(prev => ({
        ...prev,
        [field]: error
      }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!rule || !isValid) return;
    
    setIsSubmitting(true);
    try {
      await onSave(rule.id, formData);
      onClose();
    } catch (error) {
      console.error('Failed to save mapping rule:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    if (!isSubmitting) {
      onClose();
    }
  };

  if (!isOpen || !rule) return null;

  const emotionFields: Array<{ key: keyof MappingRuleUpdateRequest; label: string; color: string }> = [
    { key: 'sadPercentage', label: 'Sad', color: 'blue' },
    { key: 'fearPercentage', label: 'Fear', color: 'purple' },
    { key: 'angryPercentage', label: 'Angry', color: 'red' },
    { key: 'happyPercentage', label: 'Happy', color: 'green' },
    { key: 'conflictedPercentage', label: 'Conflicted', color: 'gray' },
    { key: 'surprisedPercentage', label: 'Surprised', color: 'yellow' }
  ];

  return (
    <div className="modal-overlay" onClick={handleClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Edit Mapping Rule</h2>
          <button 
            className="close-btn" 
            onClick={handleClose}
            disabled={isSubmitting}
          >
            ×
          </button>
        </div>
        
        <div className="modal-body">
          <div className="form-group">
            <label>Emotion Combination</label>
            <input
              type="text"
              value={rule.emotionCombination}
              disabled
              className="disabled-input"
            />
            <small className="form-help">
              This combination cannot be changed
            </small>
          </div>

          <div className="percentages-section">
            <h3>Emotion Percentages</h3>
            <p className="section-description">
              Adjust the percentage distribution for each emotion. Total must equal exactly 100%.
            </p>
            
            <div className="percentages-grid">
              {emotionFields.map(({ key, label, color }) => (
                <div key={key} className="form-group percentage-group">
                  <label className={`percentage-label ${color}`}>
                    {label} %
                  </label>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    value={formData[key]}
                    onChange={(e) => handleInputChange(key, e.target.value)}
                    onBlur={() => handleBlur(key)}
                    className={`percentage-input ${errors[key] ? 'error' : ''}`}
                    disabled={isSubmitting}
                  />
                  {errors[key] && (
                    <div className="field-error">{errors[key]}</div>
                  )}
                </div>
              ))}
            </div>
            
            <div className="total-section">
              <div className={`total-display ${totalPercentage === 100 ? 'valid' : 'invalid'}`}>
                <span className="total-label">Total:</span>
                <span className="total-value">{totalPercentage}%</span>
                {totalPercentage !== 100 && (
                  <span className="total-error">
                    {totalPercentage < 100 ? 'Add' : 'Remove'} {Math.abs(100 - totalPercentage)}% to reach 100%
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="modal-footer">
          <button 
            type="button"
            className="btn-secondary" 
            onClick={handleClose}
            disabled={isSubmitting}
          >
            Cancel
          </button>
          <button 
            type="submit"
            className="btn-primary" 
            onClick={handleSubmit}
            disabled={!isValid || isSubmitting || loading}
          >
            {isSubmitting ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default MappingRuleEditModal;
