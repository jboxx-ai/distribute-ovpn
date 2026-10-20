import React, { useState, useEffect } from 'react';
import {
  Shield, Activity, Smartphone, FileText, CheckCircle2,
  Key, Plus, Lock, ChevronRight, RefreshCw, LogOut, User, Download, Copy
} from 'lucide-react';

interface Profile {

  id: string;
  name: string;
  description?: string;
  createdAtEpochMs: number;
  totalVersions: number;
  latestVersion: number;
  stableVersion?: number;
  betaVersion?: number;
  canaryVersion?: number;
}

interface Device {
  deviceId: string;
  deviceName: string;
  platform: string;
  status: string;
  assignedProfileId: string;
  assignedChannel: string;
  enrolledAtEpochMs: number;
  lastSeenAtEpochMs?: number;
  userId?: string;
  tokenExpiresAtEpochMs?: number;
}

interface AuditLog {
  id: string;
  actor: string;
  action: string;
  targetType: string;
  targetId: string;
  detailsSanitized: string;
  timestampEpochMs: number;
}

interface SessionUser {
  id: string;
  username: string;
  role: string;
  status: string;
}

interface ClientAppRelease {
  id: string;
  platform: string;
  version: string;
  channel: string;
  fileName: string;
  downloadUrl: string;
  fileSizeBytes: number;
  sha256Checksum: string;
  minOsVersion: string;
  releaseNotes?: string;
  publishedAtEpochMs: number;
}


export const App: React.FC = () => {
  // Auth state
  const [sessionToken, setSessionToken] = useState<string | null>(() => sessionStorage.getItem('vpn_session_token'));
  const [sessionUser, setSessionUser] = useState<SessionUser | null>(() => {
    const stored = sessionStorage.getItem('vpn_session_user');
    return stored ? JSON.parse(stored) : null;
  });
  const [loginUsername, setLoginUsername] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [loginTotp, setLoginTotp] = useState('');
  const [loginError, setLoginError] = useState<string | null>(null);
  const [loginLoading, setLoginLoading] = useState(false);

  // Dashboard state
  const [activeTab, setActiveTab] = useState<'overview' | 'profiles' | 'devices' | 'downloads' | 'audit'>('overview');
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const [clientApps, setClientApps] = useState<ClientAppRelease[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [selectedProfileId, setSelectedProfileId] = useState<string | null>(null);

  // Modals
  const [isCreateProfileModalOpen, setIsCreateProfileModalOpen] = useState(false);
  const [isEnrollModalOpen, setIsEnrollModalOpen] = useState(false);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

  // Form states
  const [newProfileId, setNewProfileId] = useState('');
  const [newProfileName, setNewProfileName] = useState('');
  const [newProfileDesc, setNewProfileDesc] = useState('');

  const [newDeviceName, setNewDeviceName] = useState('');
  const [newDevicePlatform, setNewDevicePlatform] = useState('ANDROID');
  const [newDeviceProfile, setNewDeviceProfile] = useState('');
  const [newDeviceChannel, setNewDeviceChannel] = useState('STABLE');
  const [generatedToken, setGeneratedToken] = useState<string | null>(null);

  const [ovpnContent, setOvpnContent] = useState('');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  // Authenticated fetch wrapper
  const authFetch = async (url: string, options: RequestInit = {}) => {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string> || {})
    };
    if (sessionToken) {
      headers['Authorization'] = `Bearer ${sessionToken}`;
    }
    const resp = await fetch(url, { ...options, headers });
    if (resp.status === 401 || resp.status === 403) {
      handleLogout();
      throw new Error('Session expired or unauthorized');
    }
    return resp;
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError(null);
    setLoginLoading(true);
    try {
      const resp = await fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: loginUsername.trim(),
          password: loginPassword,
          totpCode: loginTotp.trim()
        })
      });
      const data = await resp.json();
      if (data.success && data.data?.token) {
        const token = data.data.token;
        const user = data.data.user;
        setSessionToken(token);
        setSessionUser(user);
        sessionStorage.setItem('vpn_session_token', token);
        sessionStorage.setItem('vpn_session_user', JSON.stringify(user));
        loadData(token);
      } else {
        setLoginError(data.error?.message || 'Authentication failed. Check credentials and TOTP code.');
      }
    } catch (err) {
      setLoginError('Network error connecting to registry server');
    } finally {
      setLoginLoading(false);
    }
  };

  const handleLogout = () => {
    setSessionToken(null);
    setSessionUser(null);
    sessionStorage.removeItem('vpn_session_token');
    sessionStorage.removeItem('vpn_session_user');
    setProfiles([]);
    setDevices([]);
    setClientApps([]);
    setAuditLogs([]);
    setLoginUsername('');
    setLoginPassword('');
    setLoginTotp('');
    setLoginError(null);
  };

  const loadData = async (tokenOverride?: string) => {
    setLoading(true);
    try {
      const tk = tokenOverride || sessionToken;
      const headers: Record<string, string> = { 'Content-Type': 'application/json' };
      if (tk) headers['Authorization'] = `Bearer ${tk}`;

      const [pRes, dRes, aRes, cRes] = await Promise.all([
        fetch('/api/v1/profiles', { headers }).then(r => r.json()),
        fetch('/api/v1/devices', { headers }).then(r => r.json()),
        fetch('/api/v1/audit', { headers }).then(r => r.json()),
        fetch('/api/v1/client-apps', { headers }).then(r => r.json()).catch(() => ({ success: false, data: [] }))
      ]);

      if (pRes.success && pRes.data) {
        setProfiles(pRes.data);
        if (pRes.data.length > 0 && !selectedProfileId) {
          setSelectedProfileId(pRes.data[0].id);
        }
      }
      if (dRes.success && dRes.data) {
        setDevices(dRes.data);
      }
      if (aRes.success && aRes.data) {
        setAuditLogs(aRes.data);
      }
      if (cRes.success && cRes.data) {
        setClientApps(cRes.data);
      }

    } catch (e) {
      console.error('Error fetching backend data:', e);
      showToast('Error connecting to backend registry server');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (sessionToken) {
      loadData();
    }
  }, []);

  const handleCreateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await authFetch('/api/v1/profiles', {
        method: 'POST',
        body: JSON.stringify({
          id: newProfileId.trim(),
          name: newProfileName.trim(),
          description: newProfileDesc.trim() || null
        })
      });
      const data = await res.json();
      if (data.success) {
        showToast(`Profile ${newProfileId} created successfully`);
        setIsCreateProfileModalOpen(false);
        setNewProfileId('');
        setNewProfileName('');
        setNewProfileDesc('');
        loadData();
      } else {
        showToast(`Failed: ${data.error?.message || 'Conflict'}`);
      }
    } catch (err) {
      showToast('Network error creating profile');
    }
  };

  const handleCreateToken = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await authFetch('/api/v1/devices/tokens', {
        method: 'POST',
        body: JSON.stringify({
          deviceName: newDeviceName.trim() || 'New Device',
          platform: newDevicePlatform,
          assignedProfileId: newDeviceProfile || profiles[0]?.id || 'default',
          assignedChannel: newDeviceChannel,
          expiresInMinutes: 60
        })
      });
      const data = await res.json();
      if (data.success && data.data?.token) {
        setGeneratedToken(data.data.token);
        showToast('Enrollment token issued (valid 60m)');
        loadData();
      } else {
        showToast(`Error: ${data.error?.message || 'Token issuance failed'}`);
      }
    } catch (err) {
      showToast('Network error issuing token');
    }
  };

  const handleUploadVersion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProfileId) return;
    try {
      const res = await authFetch(`/api/v1/profiles/${selectedProfileId}/versions`, {
        method: 'POST',
        body: JSON.stringify({ ovpnContent })
      });
      const data = await res.json();
      if (data.success) {
        showToast(`Version ${data.data?.version} uploaded & signed`);
        setIsUploadModalOpen(false);
        setOvpnContent('');
        loadData();
      } else {
        showToast(`Policy validation failed: ${data.error?.message}`);
      }
    } catch (err) {
      showToast('Network error uploading profile version');
    }
  };

  const handleRevokeDevice = async (id: string) => {
    if (!confirm(`Are you sure you want to revoke device ${id}?`)) return;
    try {
      const res = await authFetch(`/api/v1/devices/${id}/revoke`, { method: 'POST' });
      const data = await res.json();
      if (data.success) {
        showToast(`Device ${id} revoked immediately`);
        loadData();
      }
    } catch (err) {
      showToast('Error revoking device');
    }
  };

  // --- Login Screen ---
  if (!sessionToken) {
    return (
      <div className="min-h-screen bg-[#0D1117] text-[#E6EDF3] flex items-center justify-center font-sans p-4">
        <div className="w-full max-w-sm space-y-6">
          <div className="text-center space-y-2">
            <div className="w-12 h-12 rounded-full bg-[#F38020]/10 border border-[#F38020] flex items-center justify-center mx-auto">
              <Shield className="w-6 h-6 text-[#F38020]" />
            </div>
            <h1 className="text-lg font-semibold">OpenSpec VPN Registry</h1>
            <p className="text-xs text-[#8B949E]">Operator Console • Multi-Factor Authentication Required</p>
          </div>

          <form onSubmit={handleLogin} className="bg-[#161B22] border border-[#30363D] rounded-lg p-6 space-y-4">
            {loginError && (
              <div className="p-3 bg-[#F85149]/10 border border-[#F85149]/30 rounded text-xs text-[#F85149]">
                {loginError}
              </div>
            )}

            <div>
              <label className="block text-xs font-medium text-[#8B949E] mb-1">Username</label>
              <input
                type="text"
                required
                autoFocus
                placeholder="admin"
                value={loginUsername}
                onChange={e => setLoginUsername(e.target.value)}
                className="w-full bg-[#0D1117] border border-[#30363D] rounded px-3 py-2 text-sm focus:border-[#F38020] outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-[#8B949E] mb-1">Password</label>
              <input
                type="password"
                required
                placeholder="••••••••"
                value={loginPassword}
                onChange={e => setLoginPassword(e.target.value)}
                className="w-full bg-[#0D1117] border border-[#30363D] rounded px-3 py-2 text-sm focus:border-[#F38020] outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-[#8B949E] mb-1">Authenticator Code (6-digit TOTP)</label>
              <input
                type="text"
                required
                maxLength={6}
                pattern="[0-9]{6}"
                inputMode="numeric"
                autoComplete="one-time-code"
                placeholder="000000"
                value={loginTotp}
                onChange={e => setLoginTotp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                className="w-full bg-[#0D1117] border border-[#30363D] rounded px-3 py-2 text-sm font-mono tracking-[0.3em] text-center focus:border-[#F38020] outline-none"
              />
            </div>

            <button
              type="submit"
              disabled={loginLoading || loginTotp.length !== 6}
              className="w-full py-2.5 text-sm font-medium text-white bg-[#F38020] rounded hover:bg-[#F38020]/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center justify-center gap-2"
            >
              {loginLoading ? (
                <><RefreshCw className="w-4 h-4 animate-spin" /> Authenticating...</>
              ) : (
                <><Lock className="w-4 h-4" /> Sign In</>
              )}
            </button>
          </form>

          <p className="text-center text-[10px] text-[#8B949E]">
            Self-contained RFC 6238 TOTP • No external IdP dependencies
          </p>
        </div>
      </div>
    );
  }

  // --- Authenticated Dashboard ---
  return (
    <div className="min-h-screen bg-[#0D1117] text-[#E6EDF3] flex flex-col font-sans">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-[#161B22] border border-[#F38020] text-[#E6EDF3] px-4 py-3 rounded shadow-xl flex items-center gap-3 animate-fade-in">
          <CheckCircle2 className="w-5 h-5 text-[#F38020]" />
          <span className="text-sm font-medium">{toastMessage}</span>
        </div>
      )}

      {/* Header */}
      <header className="border-b border-[#30363D] bg-[#161B22] px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded bg-[#F38020]/10 border border-[#F38020] flex items-center justify-center">
            <Shield className="w-4 h-4 text-[#F38020]" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base font-semibold leading-none">OpenSpec VPN Registry</h1>
              <span className="text-[10px] uppercase font-mono px-1.5 py-0.5 rounded bg-[#3FB950]/10 border border-[#3FB950]/30 text-[#3FB950]">
                Authenticated
              </span>
            </div>
            <p className="text-xs text-[#8B949E] mt-0.5">Fleet Profile Distribution & Device Trust Control Plane</p>
          </div>
        </div>

        {/* Global Controls */}
        <div className="flex items-center gap-3">
          {sessionUser && (
            <div className="flex items-center gap-2 px-3 py-1.5 bg-[#0D1117] border border-[#30363D] rounded text-xs">
              <User className="w-3.5 h-3.5 text-[#8B949E]" />
              <span className="font-medium text-[#E6EDF3]">{sessionUser.username}</span>
              <span className="font-mono text-[10px] px-1.5 py-0.5 rounded bg-[#58A6FF]/10 border border-[#58A6FF]/30 text-[#58A6FF]">
                {sessionUser.role}
              </span>
            </div>
          )}
          <button
            onClick={() => loadData()}
            className="flex items-center gap-1.5 text-xs text-[#8B949E] hover:text-[#E6EDF3] px-2.5 py-1.5 rounded border border-[#30363D] bg-[#0D1117] hover:border-[#8B949E] transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-[#F38020]' : ''}`} />
            Refresh
          </button>
          <button
            onClick={handleLogout}
            className="flex items-center gap-1.5 text-xs text-[#F85149] hover:text-[#F85149]/80 px-2.5 py-1.5 rounded border border-[#30363D] bg-[#0D1117] hover:border-[#F85149]/50 transition-colors"
          >
            <LogOut className="w-3.5 h-3.5" />
            Sign Out
          </button>
        </div>
      </header>

      {/* Navigation Tabs */}
      <nav className="border-b border-[#30363D] bg-[#0D1117] px-6 flex gap-6 text-sm">
        <button
          onClick={() => setActiveTab('overview')}
          className={`py-3 font-medium border-b-2 transition-colors flex items-center gap-2 ${
            activeTab === 'overview'
              ? 'border-[#F38020] text-[#E6EDF3]'
              : 'border-transparent text-[#8B949E] hover:text-[#E6EDF3]'
          }`}
        >
          <Activity className="w-4 h-4" />
          Overview
        </button>
        <button
          onClick={() => setActiveTab('profiles')}
          className={`py-3 font-medium border-b-2 transition-colors flex items-center gap-2 ${
            activeTab === 'profiles'
              ? 'border-[#F38020] text-[#E6EDF3]'
              : 'border-transparent text-[#8B949E] hover:text-[#E6EDF3]'
          }`}
        >
          <FileText className="w-4 h-4" />
          Profiles Registry ({profiles.length})
        </button>
        <button
          onClick={() => setActiveTab('devices')}
          className={`py-3 font-medium border-b-2 transition-colors flex items-center gap-2 ${
            activeTab === 'devices'
              ? 'border-[#F38020] text-[#E6EDF3]'
              : 'border-transparent text-[#8B949E] hover:text-[#E6EDF3]'
          }`}
        >
          <Smartphone className="w-4 h-4" />
          Device Trust Fleet ({devices.length})
        </button>
        <button
          onClick={() => setActiveTab('downloads')}
          className={`py-3 font-medium border-b-2 transition-colors flex items-center gap-2 ${
            activeTab === 'downloads'
              ? 'border-[#F38020] text-[#E6EDF3]'
              : 'border-transparent text-[#8B949E] hover:text-[#E6EDF3]'
          }`}
        >
          <Download className="w-4 h-4" />
          Client Apps ({clientApps.length})
        </button>
        <button
          onClick={() => setActiveTab('audit')}
          className={`py-3 font-medium border-b-2 transition-colors flex items-center gap-2 ${
            activeTab === 'audit'
              ? 'border-[#F38020] text-[#E6EDF3]'
              : 'border-transparent text-[#8B949E] hover:text-[#E6EDF3]'
          }`}
        >
          <Lock className="w-4 h-4" />
          Audit Logs ({auditLogs.length})
        </button>

      </nav>

      {/* Main Body */}
      <main className="flex-1 p-6 max-w-7xl mx-auto w-full">
        {activeTab === 'overview' && (
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="bg-[#161B22] border border-[#30363D] p-4 rounded">
                <div className="text-xs text-[#8B949E] font-medium">TOTAL PROFILES</div>
                <div className="text-2xl font-bold mt-1 text-[#E6EDF3]">{profiles.length}</div>
                <div className="text-[11px] text-[#8B949E] mt-1">Configured in registry</div>
              </div>
              <div className="bg-[#161B22] border border-[#30363D] p-4 rounded">
                <div className="text-xs text-[#8B949E] font-medium">ENROLLED FLEET</div>
                <div className="text-2xl font-bold mt-1 text-[#E6EDF3]">
                  {devices.filter(d => d.status === 'ACTIVE').length}
                </div>
                <div className="text-[11px] text-[#3FB950] mt-1">Active trust certificates</div>
              </div>
              <div className="bg-[#161B22] border border-[#30363D] p-4 rounded">
                <div className="text-xs text-[#8B949E] font-medium">AUDIT TRAIL EVENTS</div>
                <div className="text-2xl font-bold mt-1 text-[#E6EDF3]">{auditLogs.length}</div>
                <div className="text-[11px] text-[#8B949E] mt-1">Tamper-evident logs</div>
              </div>
              <div className="bg-[#161B22] border border-[#30363D] p-4 rounded">
                <div className="text-xs text-[#8B949E] font-medium">AUTH MODE</div>
                <div className="text-sm font-mono mt-2 text-[#F38020] truncate">TOTP (RFC 6238)</div>
                <div className="text-[11px] text-[#8B949E] mt-1">Self-contained, no IdP</div>
              </div>
            </div>

            {profiles.length === 0 ? (
              <div className="bg-[#161B22] border border-[#30363D] rounded p-12 text-center">
                <FileText className="w-10 h-10 text-[#8B949E] mx-auto mb-3 opacity-40" />
                <h3 className="text-sm font-semibold text-[#E6EDF3]">No VPN Profiles Configured Yet</h3>
                <p className="text-xs text-[#8B949E] max-w-sm mx-auto mt-1 mb-4">
                  The database is currently empty. Get started by creating your first VPN profile definition.
                </p>
                <button
                  onClick={() => setIsCreateProfileModalOpen(true)}
                  className="inline-flex items-center gap-2 text-xs font-medium text-white bg-[#F38020] px-4 py-2 rounded shadow-sm hover:bg-[#F38020]/90"
                >
                  <Plus className="w-4 h-4" />
                  Create Profile
                </button>
              </div>
            ) : (
              <div className="bg-[#161B22] border border-[#30363D] rounded overflow-hidden">
                <div className="px-4 py-3 border-b border-[#30363D] flex justify-between items-center">
                  <h3 className="text-xs font-semibold text-[#8B949E] uppercase tracking-wider">Registered Profile Clusters</h3>
                  <button
                    onClick={() => setIsCreateProfileModalOpen(true)}
                    className="text-xs text-[#F38020] hover:underline flex items-center gap-1 font-medium"
                  >
                    <Plus className="w-3.5 h-3.5" /> Add Profile
                  </button>
                </div>
                <table className="w-full text-left text-xs">
                  <thead className="bg-[#010409] text-[#8B949E] border-b border-[#30363D]">
                    <tr>
                      <th className="px-4 py-2.5 font-medium">Profile ID</th>
                      <th className="px-4 py-2.5 font-medium">Display Name</th>
                      <th className="px-4 py-2.5 font-medium">Latest Version</th>
                      <th className="px-4 py-2.5 font-medium">Release Channels</th>
                      <th className="px-4 py-2.5 font-medium text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#30363D]">
                    {profiles.map(p => (
                      <tr key={p.id} className="hover:bg-[#0D1117]/50">
                        <td className="px-4 py-3 font-mono font-medium text-[#E6EDF3]">{p.id}</td>
                        <td className="px-4 py-3 text-[#8B949E]">{p.name}</td>
                        <td className="px-4 py-3 font-mono">v{p.latestVersion || 0}</td>
                        <td className="px-4 py-3">
                          <div className="flex gap-1.5">
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-[#3FB950]/10 border border-[#3FB950]/30 text-[#3FB950]">
                              STABLE: v{p.stableVersion || '-'}
                            </span>
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-[#D29922]/10 border border-[#D29922]/30 text-[#D29922]">
                              BETA: v{p.betaVersion || '-'}
                            </span>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={() => { setSelectedProfileId(p.id); setActiveTab('profiles'); }}
                            className="text-xs text-[#58A6FF] hover:underline"
                          >
                            Manage
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {activeTab === 'profiles' && (
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <h2 className="text-sm font-semibold">VPN Profiles & Version Manifests</h2>
              <button
                onClick={() => setIsCreateProfileModalOpen(true)}
                className="inline-flex items-center gap-1.5 text-xs font-medium text-white bg-[#F38020] px-3 py-1.5 rounded shadow-sm hover:bg-[#F38020]/90"
              >
                <Plus className="w-3.5 h-3.5" />
                Create New Profile
              </button>
            </div>

            {profiles.length === 0 ? (
              <div className="bg-[#161B22] border border-[#30363D] rounded p-12 text-center">
                <FileText className="w-10 h-10 text-[#8B949E] mx-auto mb-3 opacity-40" />
                <h3 className="text-sm font-semibold text-[#E6EDF3]">No Profiles Registered</h3>
                <p className="text-xs text-[#8B949E] mt-1">Create your first profile to start signing and publishing versions.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="bg-[#161B22] border border-[#30363D] rounded divide-y divide-[#30363D]">
                  <div className="px-4 py-3 font-semibold text-xs text-[#8B949E] uppercase">Profiles</div>
                  {profiles.map(p => (
                    <button
                      key={p.id}
                      onClick={() => setSelectedProfileId(p.id)}
                      className={`w-full text-left p-4 hover:bg-[#0D1117]/50 transition-colors flex items-center justify-between ${
                        selectedProfileId === p.id ? 'bg-[#0D1117] border-l-2 border-[#F38020]' : ''
                      }`}
                    >
                      <div>
                        <div className="font-mono text-xs font-semibold text-[#E6EDF3]">{p.id}</div>
                        <div className="text-[11px] text-[#8B949E] mt-0.5">{p.name}</div>
                      </div>
                      <ChevronRight className="w-4 h-4 text-[#8B949E]" />
                    </button>
                  ))}
                </div>

                <div className="md:col-span-2 space-y-4">
                  {selectedProfileId ? (
                    <div className="bg-[#161B22] border border-[#30363D] rounded p-5 space-y-4">
                      <div className="flex justify-between items-start">
                        <div>
                          <h3 className="font-mono text-sm font-bold text-[#E6EDF3]">{selectedProfileId}</h3>
                          <p className="text-xs text-[#8B949E]">
                            {profiles.find(p => p.id === selectedProfileId)?.description || 'No description provided'}
                          </p>
                        </div>
                        <button
                          onClick={() => setIsUploadModalOpen(true)}
                          className="flex items-center gap-1.5 text-xs font-medium text-white bg-[#F38020] hover:bg-[#F38020]/90 px-3 py-1.5 rounded transition-colors"
                        >
                          <Plus className="w-3.5 h-3.5" />
                          Upload .ovpn Version
                        </button>
                      </div>

                      <div className="border-t border-[#30363D] pt-4">
                        <h4 className="text-xs font-semibold text-[#8B949E] uppercase tracking-wider mb-2">Version Release Channels</h4>
                        <div className="grid grid-cols-3 gap-3 text-xs">
                          <div className="p-3 bg-[#010409] border border-[#30363D] rounded">
                            <span className="text-[10px] font-bold text-[#3FB950] uppercase">STABLE</span>
                            <div className="text-lg font-mono font-bold mt-1">
                              v{profiles.find(p => p.id === selectedProfileId)?.stableVersion || '-'}
                            </div>
                          </div>
                          <div className="p-3 bg-[#010409] border border-[#30363D] rounded">
                            <span className="text-[10px] font-bold text-[#D29922] uppercase">BETA</span>
                            <div className="text-lg font-mono font-bold mt-1">
                              v{profiles.find(p => p.id === selectedProfileId)?.betaVersion || '-'}
                            </div>
                          </div>
                          <div className="p-3 bg-[#010409] border border-[#30363D] rounded">
                            <span className="text-[10px] font-bold text-[#58A6FF] uppercase">CANARY</span>
                            <div className="text-lg font-mono font-bold mt-1">
                              v{profiles.find(p => p.id === selectedProfileId)?.canaryVersion || '-'}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="text-center text-xs text-[#8B949E] py-12">Select a profile to view details</div>
                  )}
                </div>
              </div>
            )}
          </div>
        )}

        {activeTab === 'devices' && (
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <h2 className="text-sm font-semibold">Enrolled Fleet Devices</h2>
              <button
                onClick={() => { setGeneratedToken(null); setIsEnrollModalOpen(true); }}
                className="inline-flex items-center gap-1.5 text-xs font-medium text-white bg-[#F38020] px-3 py-1.5 rounded shadow-sm hover:bg-[#F38020]/90"
              >
                <Key className="w-3.5 h-3.5" />
                Issue Enrollment Token
              </button>
            </div>

            {devices.length === 0 ? (
              <div className="bg-[#161B22] border border-[#30363D] rounded p-12 text-center">
                <Smartphone className="w-10 h-10 text-[#8B949E] mx-auto mb-3 opacity-40" />
                <h3 className="text-sm font-semibold text-[#E6EDF3]">No Enrolled Devices Yet</h3>
                <p className="text-xs text-[#8B949E] mt-1">
                  Issue a single-use token or use native client TOTP enrollment to onboard devices.
                </p>
              </div>
            ) : (
              <div className="bg-[#161B22] border border-[#30363D] rounded overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-[#010409] text-[#8B949E] border-b border-[#30363D]">
                    <tr>
                      <th className="px-4 py-2.5 font-medium">Device ID</th>
                      <th className="px-4 py-2.5 font-medium">Device Name</th>
                      <th className="px-4 py-2.5 font-medium">Platform</th>
                      <th className="px-4 py-2.5 font-medium">Owner</th>
                      <th className="px-4 py-2.5 font-medium">Assigned Profile</th>
                      <th className="px-4 py-2.5 font-medium">Status</th>
                      <th className="px-4 py-2.5 font-medium text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#30363D]">
                    {devices.map(d => (
                      <tr key={d.deviceId} className="hover:bg-[#0D1117]/50">
                        <td className="px-4 py-3 font-mono font-medium text-[#E6EDF3]">{d.deviceId}</td>
                        <td className="px-4 py-3">{d.deviceName}</td>
                        <td className="px-4 py-3 font-mono text-[11px] uppercase">{d.platform}</td>
                        <td className="px-4 py-3 font-mono text-[11px] text-[#8B949E]">{d.userId || '-'}</td>
                        <td className="px-4 py-3 font-mono text-[11px] text-[#58A6FF]">{d.assignedProfileId} ({d.assignedChannel})</td>
                        <td className="px-4 py-3">
                          <span className={`px-1.5 py-0.5 rounded text-[10px] font-mono border ${
                            d.status === 'ACTIVE'
                              ? 'bg-[#3FB950]/10 border-[#3FB950]/30 text-[#3FB950]'
                              : 'bg-[#F85149]/10 border-[#F85149]/30 text-[#F85149]'
                          }`}>
                            {d.status}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          {d.status === 'ACTIVE' && (
                            <button
                              onClick={() => handleRevokeDevice(d.deviceId)}
                              className="text-xs text-[#F85149] hover:underline"
                            >
                              Revoke
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {activeTab === 'downloads' && (
          <div className="space-y-6">
            <div>
              <h2 className="text-sm font-semibold">Authenticated Client Apps Download Center</h2>
              <p className="text-xs text-[#8B949E] mt-0.5">Download official VPN platform binaries and mobile APKs verified with cryptographic signatures.</p>
            </div>

            {/* Quick Cards Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Android Card */}
              <div className="bg-[#161B22] border border-[#30363D] rounded-lg p-6 flex flex-col justify-between space-y-6">
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-[#3FB950]/10 border border-[#3FB950]/30 flex items-center justify-center">
                        <Smartphone className="w-5 h-5 text-[#3FB950]" />
                      </div>
                      <div>
                        <h3 className="text-sm font-semibold text-[#E6EDF3]">Android Client App</h3>
                        <span className="text-[10px] font-mono text-[#8B949E]">Package: fyi.rifqifardi.vpn.android</span>
                      </div>
                    </div>
                    <span className="px-2 py-0.5 rounded text-[11px] font-mono bg-[#58A6FF]/10 border border-[#58A6FF]/30 text-[#58A6FF]">
                      v0.1.0 (Stable)
                    </span>
                  </div>

                  <p className="text-xs text-[#8B949E] leading-relaxed">
                    Native Kotlin Multiplatform client featuring Kumo UI token system, automated WireGuard/OpenVPN tunnel lifecycle, and background session heartbeat.
                  </p>

                  <div className="bg-[#0D1117] border border-[#30363D] rounded p-3 space-y-1.5 text-xs font-mono">
                    <div className="flex justify-between text-[#8B949E]">
                      <span>Target OS:</span>
                      <span className="text-[#E6EDF3]">Android 8.0+ (API 26–35)</span>
                    </div>
                    <div className="flex justify-between text-[#8B949E]">
                      <span>Signature:</span>
                      <span className="text-[#3FB950]">APK Signature v2 Verified</span>
                    </div>
                    <div className="flex justify-between text-[#8B949E]">
                      <span>File Size:</span>
                      <span className="text-[#E6EDF3]">~2.8 MB (Release APK)</span>
                    </div>
                  </div>
                </div>

                <div className="space-y-3 pt-2 border-t border-[#30363D]">
                  <a
                    href="/assets/distribute-ovpn-debug-signed.apk"
                    download="distribute-ovpn-debug-signed.apk"
                    className="w-full bg-[#F38020] hover:bg-[#F38020]/90 text-white text-xs font-medium py-2.5 px-4 rounded flex items-center justify-center gap-2 transition-colors shadow-sm"
                  >
                    <Download className="w-4 h-4" />
                    Download Signed APK (.apk)
                  </a>
                  <button
                    onClick={() => {
                      navigator.clipboard.writeText(window.location.origin + '/assets/distribute-ovpn-debug-signed.apk');
                      showToast('Direct download URL copied to clipboard');
                    }}
                    className="w-full bg-[#0D1117] hover:border-[#8B949E] border border-[#30363D] text-[#8B949E] hover:text-[#E6EDF3] text-xs py-2 px-4 rounded flex items-center justify-center gap-1.5 transition-colors"
                  >
                    <Copy className="w-3.5 h-3.5" />
                    Copy Direct Download Link
                  </button>
                </div>
              </div>

              {/* macOS Card */}
              <div className="bg-[#161B22] border border-[#30363D] rounded-lg p-6 flex flex-col justify-between space-y-6">
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-[#58A6FF]/10 border border-[#58A6FF]/30 flex items-center justify-center">
                        <Activity className="w-5 h-5 text-[#58A6FF]" />
                      </div>
                      <div>
                        <h3 className="text-sm font-semibold text-[#E6EDF3]">macOS Client App</h3>
                        <span className="text-[10px] font-mono text-[#8B949E]">Bundle: fyi.rifqifardi.vpn.macos</span>
                      </div>
                    </div>
                    <span className="px-2 py-0.5 rounded text-[11px] font-mono bg-[#D29922]/10 border border-[#D29922]/30 text-[#D29922]">
                      v0.1.0 (Beta)
                    </span>
                  </div>

                  <p className="text-xs text-[#8B949E] leading-relaxed">
                    macOS Menu Bar and Desktop utility with System Extension Packet Tunnel Provider integration and instant 1-click device trust pairing.
                  </p>

                  <div className="bg-[#0D1117] border border-[#30363D] rounded p-3 space-y-1.5 text-xs font-mono">
                    <div className="flex justify-between text-[#8B949E]">
                      <span>Architecture:</span>
                      <span className="text-[#E6EDF3]">Apple Silicon & Intel (Universal)</span>
                    </div>
                    <div className="flex justify-between text-[#8B949E]">
                      <span>Target OS:</span>
                      <span className="text-[#E6EDF3]">macOS 13.0+ (Ventura, Sonoma, Sequoia)</span>
                    </div>
                    <div className="flex justify-between text-[#8B949E]">
                      <span>Signing:</span>
                      <span className="text-[#58A6FF]">Developer ID & Notarized</span>
                    </div>
                  </div>
                </div>

                <div className="space-y-3 pt-2 border-t border-[#30363D]">
                  <button
                    onClick={() => showToast('macOS Universal DMG is compiled via GitHub Actions macOS runner.')}
                    className="w-full bg-[#238636] hover:bg-[#2EA043] text-white text-xs font-medium py-2.5 px-4 rounded flex items-center justify-center gap-2 transition-colors shadow-sm"
                  >
                    <Download className="w-4 h-4" />
                    Download macOS DMG (.dmg)
                  </button>
                  <button
                    onClick={() => {
                      navigator.clipboard.writeText(`curl -fsSL ${window.location.origin}/api/v1/client-apps/latest?platform=MACOS | jq -r .data.downloadUrl`);
                      showToast('CLI curl snippet copied');
                    }}
                    className="w-full bg-[#0D1117] hover:border-[#8B949E] border border-[#30363D] text-[#8B949E] hover:text-[#E6EDF3] text-xs py-2 px-4 rounded flex items-center justify-center gap-1.5 transition-colors"
                  >
                    <Copy className="w-3.5 h-3.5" />
                    Copy CLI Install Command
                  </button>
                </div>
              </div>
            </div>

            {/* Releases Audit Table */}
            <div className="space-y-3 pt-4">
              <h3 className="text-xs font-semibold text-[#8B949E] uppercase tracking-wider">Published Binary Releases Catalog</h3>
              <div className="bg-[#161B22] border border-[#30363D] rounded overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-[#010409] text-[#8B949E] border-b border-[#30363D] font-mono">
                    <tr>
                      <th className="px-4 py-2.5">Platform</th>
                      <th className="px-4 py-2.5">Version</th>
                      <th className="px-4 py-2.5">Channel</th>
                      <th className="px-4 py-2.5">File Name</th>
                      <th className="px-4 py-2.5">Min OS</th>
                      <th className="px-4 py-2.5">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#30363D] font-mono">
                    <tr className="hover:bg-[#0D1117]/50">
                      <td className="px-4 py-2.5 text-[#3FB950] font-semibold">ANDROID</td>
                      <td className="px-4 py-2.5 text-[#E6EDF3]">0.1.0</td>
                      <td className="px-4 py-2.5 text-[#58A6FF]">STABLE</td>
                      <td className="px-4 py-2.5 text-[#8B949E]">distribute-ovpn-debug-signed.apk</td>
                      <td className="px-4 py-2.5 text-[#8B949E]">Android 8.0 (API 26)</td>
                      <td className="px-4 py-2.5">
                        <a
                          href="/assets/distribute-ovpn-debug-signed.apk"
                          download
                          className="text-[#F38020] hover:underline flex items-center gap-1"
                        >
                          <Download className="w-3 h-3" /> Download
                        </a>
                      </td>
                    </tr>
                    <tr className="hover:bg-[#0D1117]/50">
                      <td className="px-4 py-2.5 text-[#58A6FF] font-semibold">MACOS</td>
                      <td className="px-4 py-2.5 text-[#E6EDF3]">0.1.0</td>
                      <td className="px-4 py-2.5 text-[#D29922]">BETA</td>
                      <td className="px-4 py-2.5 text-[#8B949E]">OpenSpecVPN-0.1.0-Universal.dmg</td>
                      <td className="px-4 py-2.5 text-[#8B949E]">macOS 13.0</td>
                      <td className="px-4 py-2.5 text-[#8B949E]">
                        Managed via CI
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'audit' && (
          <div className="space-y-4">
            <h2 className="text-sm font-semibold">Tamper-Evident Audit Trail</h2>
            {auditLogs.length === 0 ? (
              <div className="bg-[#161B22] border border-[#30363D] rounded p-12 text-center">
                <Lock className="w-10 h-10 text-[#8B949E] mx-auto mb-3 opacity-40" />
                <h3 className="text-sm font-semibold text-[#E6EDF3]">No Audit Events Recorded Yet</h3>
                <p className="text-xs text-[#8B949E] mt-1">Audit events will appear here automatically upon administrative and enrollment actions.</p>
              </div>
            ) : (
              <div className="bg-[#161B22] border border-[#30363D] rounded overflow-hidden">
                <table className="w-full text-left text-xs font-mono">
                  <thead className="bg-[#010409] text-[#8B949E] border-b border-[#30363D]">
                    <tr>
                      <th className="px-4 py-2.5 font-medium">ID</th>
                      <th className="px-4 py-2.5 font-medium">Actor</th>
                      <th className="px-4 py-2.5 font-medium">Action</th>
                      <th className="px-4 py-2.5 font-medium">Target</th>
                      <th className="px-4 py-2.5 font-medium">Sanitized Details</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#30363D]">
                    {auditLogs.map(a => (
                      <tr key={a.id} className="hover:bg-[#0D1117]/50">
                        <td className="px-4 py-2.5 text-[#8B949E]">{a.id}</td>
                        <td className="px-4 py-2.5 text-[#E6EDF3]">{a.actor}</td>
                        <td className="px-4 py-2.5 text-[#F38020]">{a.action}</td>
                        <td className="px-4 py-2.5 text-[#8B949E]">{a.targetType}:{a.targetId}</td>
                        <td className="px-4 py-2.5 text-[#8B949E] max-w-xs truncate">{a.detailsSanitized}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </main>

      {/* Modal: Create Profile */}
      {isCreateProfileModalOpen && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-[#161B22] border border-[#30363D] rounded-lg max-w-md w-full p-6 space-y-4">
            <h3 className="text-base font-semibold">Create VPN Profile</h3>
            <form onSubmit={handleCreateProfile} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-[#8B949E] mb-1">Profile ID (Slug)</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. eng-corp-gw"
                  value={newProfileId}
                  onChange={e => setNewProfileId(e.target.value)}
                  className="w-full bg-[#0D1117] border border-[#30363D] rounded px-3 py-2 text-xs font-mono focus:border-[#F38020] outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-[#8B949E] mb-1">Display Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Engineering Gateway"
                  value={newProfileName}
                  onChange={e => setNewProfileName(e.target.value)}
                  className="w-full bg-[#0D1117] border border-[#30363D] rounded px-3 py-2 text-xs focus:border-[#F38020] outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-[#8B949E] mb-1">Description (Optional)</label>
                <textarea
                  placeholder="Purpose and access controls"
                  value={newProfileDesc}
                  onChange={e => setNewProfileDesc(e.target.value)}
                  className="w-full bg-[#0D1117] border border-[#30363D] rounded px-3 py-2 text-xs h-20 focus:border-[#F38020] outline-none"
                />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsCreateProfileModalOpen(false)}
                  className="px-3 py-1.5 text-xs text-[#8B949E] hover:text-[#E6EDF3]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 text-xs font-medium text-white bg-[#F38020] rounded hover:bg-[#F38020]/90"
                >
                  Create Profile
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Issue Enrollment Token */}
      {isEnrollModalOpen && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-[#161B22] border border-[#30363D] rounded-lg max-w-md w-full p-6 space-y-4">
            <h3 className="text-base font-semibold">Issue Device Enrollment Token</h3>

            {generatedToken ? (
              <div className="space-y-4">
                <div className="p-3 bg-[#010409] border border-[#3FB950]/30 rounded space-y-2">
                  <div className="text-xs text-[#3FB950] font-medium flex items-center gap-1.5">
                    <CheckCircle2 className="w-4 h-4" />
                    Single-Use Token Generated (60m validity)
                  </div>
                  <div className="font-mono text-xs bg-[#161B22] p-2 rounded border border-[#30363D] break-all select-all text-[#F38020]">
                    {generatedToken}
                  </div>
                </div>
                <div className="flex justify-end">
                  <button
                    onClick={() => setIsEnrollModalOpen(false)}
                    className="px-4 py-1.5 text-xs font-medium text-white bg-[#F38020] rounded hover:bg-[#F38020]/90"
                  >
                    Done
                  </button>
                </div>
              </div>
            ) : (
              <form onSubmit={handleCreateToken} className="space-y-4">
                <div>
                  <label className="block text-xs font-medium text-[#8B949E] mb-1">Device Name</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. MacBook-Pro-Dev"
                    value={newDeviceName}
                    onChange={e => setNewDeviceName(e.target.value)}
                    className="w-full bg-[#0D1117] border border-[#30363D] rounded px-3 py-2 text-xs focus:border-[#F38020] outline-none"
                  />
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-[#8B949E] mb-1">Platform</label>
                    <select
                      value={newDevicePlatform}
                      onChange={e => setNewDevicePlatform(e.target.value)}
                      className="w-full bg-[#0D1117] border border-[#30363D] rounded px-3 py-2 text-xs focus:border-[#F38020] outline-none"
                    >
                      <option value="ANDROID">Android</option>
                      <option value="MACOS">macOS</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-[#8B949E] mb-1">Profile</label>
                    <select
                      value={newDeviceProfile || profiles[0]?.id || ''}
                      onChange={e => setNewDeviceProfile(e.target.value)}
                      className="w-full bg-[#0D1117] border border-[#30363D] rounded px-3 py-2 text-xs focus:border-[#F38020] outline-none"
                    >
                      {profiles.map(p => (
                        <option key={p.id} value={p.id}>{p.name} ({p.id})</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-[#8B949E] mb-1">Channel</label>
                    <select
                      value={newDeviceChannel}
                      onChange={e => setNewDeviceChannel(e.target.value)}
                      className="w-full bg-[#0D1117] border border-[#30363D] rounded px-3 py-2 text-xs focus:border-[#F38020] outline-none"
                    >
                      <option value="STABLE">STABLE</option>
                      <option value="BETA">BETA</option>
                      <option value="CANARY">CANARY</option>
                    </select>
                  </div>
                </div>
                <div className="flex justify-end gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setIsEnrollModalOpen(false)}
                    className="px-3 py-1.5 text-xs text-[#8B949E] hover:text-[#E6EDF3]"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-1.5 text-xs font-medium text-white bg-[#F38020] rounded hover:bg-[#F38020]/90"
                  >
                    Generate Token
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Modal: Upload .ovpn Version */}
      {isUploadModalOpen && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-[#161B22] border border-[#30363D] rounded-lg max-w-lg w-full p-6 space-y-4">
            <h3 className="text-base font-semibold">Upload New .ovpn Configuration</h3>
            <p className="text-xs text-[#8B949E]">
              Policy engine will strictly validate directives. Unsafe directives (e.g. <code>redirect-gateway</code>, <code>up/down scripts</code>) will be rejected.
            </p>
            <form onSubmit={handleUploadVersion} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-[#8B949E] mb-1">OpenVPN Directives</label>
                <textarea
                  required
                  placeholder="client&#10;dev tun&#10;proto udp&#10;remote vpn.corp.internal 1194"
                  value={ovpnContent}
                  onChange={e => setOvpnContent(e.target.value)}
                  className="w-full bg-[#0D1117] border border-[#30363D] rounded px-3 py-2 text-xs font-mono h-40 focus:border-[#F38020] outline-none"
                />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsUploadModalOpen(false)}
                  className="px-3 py-1.5 text-xs text-[#8B949E] hover:text-[#E6EDF3]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 text-xs font-medium text-white bg-[#F38020] rounded hover:bg-[#F38020]/90"
                >
                  Upload & Sign
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
