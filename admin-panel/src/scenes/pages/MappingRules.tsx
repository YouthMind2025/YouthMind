import React, { useState, useEffect } from 'react';
import { mappingRulesApi } from '../../services/api';
import { MappingRule, MappingRuleUpdateRequest } from '../../types';
import MappingRuleEditModal from '../components/MappingRuleEditModal';
import './MappingRules.css';

const MappingRules: React.FC = () => {
  const [rules, setRules] = useState<MappingRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editingRule, setEditingRule] = useState<MappingRule | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadRules();
  }, []);

  const loadRules = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await mappingRulesApi.getAllRules();
      setRules(response.data.data || response.data);
    } catch (err) {
      console.error('Failed to load mapping rules:', err);
      setError('Failed to load mapping rules. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleEditRule = (rule: MappingRule) => {
    setEditingRule(rule);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingRule(null);
  };

  const handleSaveRule = async (ruleId: string, data: MappingRuleUpdateRequest) => {
    try {
      setSaving(true);
      const response = await mappingRulesApi.updateRule(ruleId, data);
      
      // Update the rule in the local state
      setRules(prevRules => 
        prevRules.map(rule => 
          rule.id === ruleId ? response.data.data || response.data : rule
        )
      );
      
      handleCloseModal();
    } catch (err) {
      console.error('Failed to save mapping rule:', err);
      throw err; // Re-throw to let the modal handle the error
    } finally {
      setSaving(false);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const isRecentlyUpdated = (updatedAt: string) => {
    const updateDate = new Date(updatedAt);
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
    return updateDate > sevenDaysAgo;
  };

  const isDefaultRule = (rule: MappingRule) => {
    // Check if rule uses the default distribution (20, 5, 20, 20, 20, 15)
    const defaultPercentages = [20, 5, 20, 20, 20, 15];
    const rulePercentages = [
      rule.sadPercentage,
      rule.fearPercentage,
      rule.angryPercentage,
      rule.happyPercentage,
      rule.conflictedPercentage,
      rule.surprisedPercentage
    ];
    
    return rulePercentages.every((percentage, index) => percentage === defaultPercentages[index]);
  };

  const getRuleBadges = (rule: MappingRule) => {
    const badges = [];
    
    if (isRecentlyUpdated(rule.updatedAt)) {
      badges.push({ type: 'recently-updated', text: 'Recently Updated' });
    }
    
    if (isDefaultRule(rule)) {
      badges.push({ type: 'default-rule', text: 'Default' });
    } else {
      badges.push({ type: 'custom-rule', text: 'Custom' });
    }
    
    // Check if this is a commonly used combination
    const commonCombinations = ['sad', 'fear', 'angry', 'happy', 'sad,fear', 'fear,angry', 'happy,surprised'];
    if (commonCombinations.includes(rule.emotionCombination)) {
      badges.push({ type: 'in-use', text: 'In Use' });
    }
    
    return badges;
  };

  const getTotalPercentage = (rule: MappingRule) => {
    return rule.sadPercentage + rule.fearPercentage + rule.angryPercentage + 
           rule.happyPercentage + rule.conflictedPercentage + rule.surprisedPercentage;
  };

  if (loading) {
    return (
      <div className="page-container">
        <div className="page-header">
          <h1 className="page-title">Mapping Rules Management</h1>
        </div>
        <div className="loading">
          <div className="loading-spinner"></div>
          Loading mapping rules...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-container">
        <div className="page-header">
          <h1 className="page-title">Mapping Rules Management</h1>
        </div>
        <div className="error">
          <span className="error-icon">⚠️</span>
          {error}
          <button className="refresh-button" onClick={loadRules}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (rules.length === 0) {
    return (
      <div className="page-container">
        <div className="page-header">
          <h1 className="page-title">Mapping Rules Management</h1>
          <p className="page-subtitle">
            Manage the 20 emotion-based recommendation mapping rules that control content distribution
          </p>
          <div className="header-actions">
            <button 
              className="refresh-button" 
              onClick={loadRules}
              disabled={loading}
            >
              🔄 Refresh
            </button>
          </div>
        </div>
        <div className="page-content">
          <div className="stats-section">
            <h2>No Mapping Rules Found</h2>
            <p>The mapping rules should be automatically created when the system starts. If you're seeing this message, please contact your system administrator.</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <h1 className="page-title">Mapping Rules Management</h1>
        <p className="page-subtitle">
          Manage the 20 emotion-based recommendation mapping rules that control content distribution
        </p>
        <div className="header-actions">
          <button 
            className="refresh-button" 
            onClick={loadRules}
            disabled={loading}
          >
            🔄 Refresh
          </button>
        </div>
      </div>

      <div className="page-content">
        <div className="stats-section">
          <h2>Overview</h2>
          <div className="stats-grid">
            <div className="stat-card">
              <h3>{rules.length}</h3>
              <div className="stat-value">{rules.length}</div>
              <div className="stat-description">Total Rules</div>
            </div>
            <div className="stat-card">
              <h3>{rules.filter(rule => isRecentlyUpdated(rule.updatedAt)).length}</h3>
              <div className="stat-value">{rules.filter(rule => isRecentlyUpdated(rule.updatedAt)).length}</div>
              <div className="stat-description">Recently Updated</div>
            </div>
            <div className="stat-card">
              <h3>{rules.filter(rule => !isDefaultRule(rule)).length}</h3>
              <div className="stat-value">{rules.filter(rule => !isDefaultRule(rule)).length}</div>
              <div className="stat-description">Custom Rules</div>
            </div>
            <div className="stat-card">
              <h3>{rules.filter(rule => isDefaultRule(rule)).length}</h3>
              <div className="stat-value">{rules.filter(rule => isDefaultRule(rule)).length}</div>
              <div className="stat-description">Default Rules</div>
            </div>
          </div>
        </div>

        <div className="rules-table">
          <table>
            <thead>
              <tr>
                <th>Emotion Combination</th>
                <th>Sad %</th>
                <th>Fear %</th>
                <th>Angry %</th>
                <th>Happy %</th>
                <th>Conflicted %</th>
                <th>Surprised %</th>
                <th>Total %</th>
                <th>Last Modified</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rules.map((rule) => {
                const badges = getRuleBadges(rule);
                const total = getTotalPercentage(rule);
                
                return (
                  <tr key={rule.id}>
                    <td className="combination-cell">
                      {rule.emotionCombination}
                    </td>
                    <td className="percentage-cell">{rule.sadPercentage}%</td>
                    <td className="percentage-cell">{rule.fearPercentage}%</td>
                    <td className="percentage-cell">{rule.angryPercentage}%</td>
                    <td className="percentage-cell">{rule.happyPercentage}%</td>
                    <td className="percentage-cell">{rule.conflictedPercentage}%</td>
                    <td className="percentage-cell">{rule.surprisedPercentage}%</td>
                    <td className="total-cell">{total}%</td>
                    <td className="last-modified-cell">
                      {formatDate(rule.updatedAt)}
                    </td>
                    <td>
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.25rem' }}>
                        {badges.map((badge, index) => (
                          <span key={index} className={`badge ${badge.type}`}>
                            {badge.text}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="actions-cell">
                      <button
                        className="edit-button"
                        onClick={() => handleEditRule(rule)}
                        disabled={saving}
                      >
                        Edit
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      <MappingRuleEditModal
        rule={editingRule}
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        onSave={handleSaveRule}
        loading={saving}
      />
    </div>
  );
};

export default MappingRules;
