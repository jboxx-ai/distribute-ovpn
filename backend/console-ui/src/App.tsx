import React, { useState } from 'react';
import { 
  Shield, Activity, Smartphone, Laptop, FileText, CheckCircle2, 
  XCircle, Key, Copy, Plus, Lock, ChevronRight
} from 'lucide-react';

interface ProfileVersion {
  version: number;
  digest: string;
  channel: 'STABLE' | 'BETA' | 'CANARY';
  createdAt: string;
}

interface Profile {
  id: string;
  name: string;
  stableVersion: number;
  betaVersion: number;
  totalVersions: number;
  activeDevices: number;
  versions: ProfileVersion[];
}

interface Device {
  id: string;
  name: string;
  platform: 'android' | 'macos';
  profileId: string;
  channel: string;
  status: 'ACTIVE' | 'REVOKED';
  lastSeen: string;
  publicKey: string;
}

interface AuditLog {
  id: string;
  timestamp: string;
  actor: string;
  action: string;
  target: string;
  status: 'SUCCESS' | 'WARNING' | 'REJECTED';
  redactedPayload: string;
}

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'overview' | 'profiles' | 'devices' | 'audit'>('overview');
  const [selectedProfileId, setSelectedProfileId] = useState<string>('prf_edge_prod');
  const [isEnrollModalOpen, setIsEnrollModalOpen] = useState(false);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  const [profiles, setProfiles] = useState<Profile[]>([
    {
      id: 'prf_edge_prod',
      name: 'edge-production-eu',
      stableVersion: 8,
      betaVersion: 9,
      totalVersions: 9,
      activeDevices: 142,
      versions: [
        { version: 9, digest: 'sha256:6f94a8c1e2b34f78...', channel: 'BETA', createdAt: '2026-10-08 01:15' },
        { version: 8, digest: 'sha256:3a89e1b2c4d5e6f7...', channel: 'STABLE', createdAt: '2026-10-01 09:30' },
        { version: 7, digest: 'sha256:1f42e3b4a5c6d7e8...', channel: 'CANARY', createdAt: '2026-09-20 14:22' }
      ]
    },
    {
      id: 'prf_corp_internal',
      name: 'corp-internal-us',
      stableVersion: 4,
      betaVersion: 4,
      totalVersions: 4,
      activeDevices: 89,
      versions: [
        { version: 4, digest: 'sha256:8b45c2d1e3f6a7b8...', channel: 'STABLE', createdAt: '2026-09-28 11:05' }
      ]
    }
  ]);

  const [devices, setDevices] = useState<Device[]>([
    { id: 'dev_m3_max_01', name: "Alex's MacBook Pro M3", platform: 'macos', profileId: 'prf_edge_prod', channel: 'STABLE', status: 'ACTIVE', lastSeen: '2 mins ago', publicKey: 'Ed25519:8a72...4e1f' },
    { id: 'dev_pixel_9_02', name: "Pixel 9 Pro (SecOps)", platform: 'android', profileId: 'prf_edge_prod', channel: 'BETA', status: 'ACTIVE', lastSeen: 'Just now', publicKey: 'Ed25519:9f34...1b8c' },
    { id: 'dev_mac_mini_ci', name: "Sandbox CI Runner 01", platform: 'macos', profileId: 'prf_corp_internal', channel: 'STABLE', status: 'REVOKED', lastSeen: '2 days ago', publicKey: 'Ed25519:5c12...3e90' }
  ]);

  const [auditLogs] = useState<AuditLog[]>([
    { id: 'aud_1', timestamp: '2026-10-08 02:10:45', actor: 'secops-admin', action: 'PROMOTE_VERSION', target: 'prf_edge_prod/v9 -> BETA', status: 'SUCCESS', redactedPayload: '{"profile":"edge-production-eu","version":9,"token":"[REDACTED_SECRET]"}' },
    { id: 'aud_2', timestamp: '2026-10-08 01:15:12', actor: 'ci-pipeline', action: 'INGEST_OVPN', target: 'prf_edge_prod/v9', status: 'SUCCESS', redactedPayload: '{"artifact_size":1420,"cert":"[REDACTED_CERT_PAYLOAD]"}' },
    { id: 'aud_3', timestamp: '2026-10-07 19:40:02', actor: 'secops-admin', action: 'REVOKE_DEVICE', target: 'dev_mac_mini_ci', status: 'WARNING', redactedPayload: '{"device_id":"dev_mac_mini_ci","reason":"Key rotation policy"}' }
  ]);

  const selectedProfile = profiles.find(p => p.id === selectedProfileId) || profiles[0];

  const handlePromote = (versionNum: number, newChannel: 'STABLE' | 'BETA' | 'CANARY') => {
    setProfiles(prev => prev.map(p => {
      if (p.id !== selectedProfile.id) return p;
      const updatedVersions = p.versions.map(v => v.version === versionNum ? { ...v, channel: newChannel } : v);
      return {
        ...p,
        stableVersion: newChannel === 'STABLE' ? versionNum : p.stableVersion,
        betaVersion: newChannel === 'BETA' ? versionNum : p.betaVersion,
        versions: updatedVersions
      };
    }));
    showToast(`Version v${versionNum} successfully promoted to ${newChannel}`);
  };

  const handleRevokeDevice = (id: string) => {
    setDevices(prev => prev.map(d => d.id === id ? { ...d, status: 'REVOKED' } : d));
    showToast(`Device ${id} access revoked immediately.`);
  };

  return (
    <div className="min-h-screen flex bg-[#0D1117] text-[#E6EDF3] font-sans antialiased">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-5 right-5 z-50 flex items-center gap-3 px-4 py-3 bg-[#161B22] border border-[#3FB950] text-[#E6EDF3] rounded-lg shadow-xl animate-fade-in">
          <CheckCircle2 className="w-5 h-5 text-[#3FB950]" />
          <span className="text-sm font-medium">{toastMessage}</span>
        </div>
      )}

      {/* Sidebar Navigation */}
      <aside className="w-64 border-r border-[#30363D] bg-[#161B22] flex flex-col justify-between">
        <div>
          <div className="p-6 flex items-center gap-3 border-b border-[#30363D]">
            <div className="p-2 bg-[#F38020]/10 border border-[#F38020]/30 rounded-lg">
              <Shield className="w-6 h-6 text-[#F38020]" />
            </div>
            <div>
              <h1 className="font-semibold text-sm tracking-tight text-white">OpenSpec VPN</h1>
              <span className="text-xs text-[#8B949E]">Registry Console</span>
            </div>
          </div>

          <nav className="p-4 space-y-1">
            <button
              onClick={() => setActiveTab('overview')}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                activeTab === 'overview' ? 'bg-[#30363D] text-white' : 'text-[#8B949E] hover:bg-[#21262D] hover:text-[#E6EDF3]'
              }`}
            >
              <Activity className="w-4 h-4 text-[#F38020]" />
              Overview
            </button>
            <button
              onClick={() => setActiveTab('profiles')}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                activeTab === 'profiles' ? 'bg-[#30363D] text-white' : 'text-[#8B949E] hover:bg-[#21262D] hover:text-[#E6EDF3]'
              }`}
            >
              <FileText className="w-4 h-4 text-[#3FB950]" />
              Profile Registry
            </button>
            <button
              onClick={() => setActiveTab('devices')}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                activeTab === 'devices' ? 'bg-[#30363D] text-white' : 'text-[#8B949E] hover:bg-[#21262D] hover:text-[#E6EDF3]'
              }`}
            >
              <Smartphone className="w-4 h-4 text-[#58A6FF]" />
              Device Fleet
            </button>
            <button
              onClick={() => setActiveTab('audit')}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                activeTab === 'audit' ? 'bg-[#30363D] text-white' : 'text-[#8B949E] hover:bg-[#21262D] hover:text-[#E6EDF3]'
              }`}
            >
              <Lock className="w-4 h-4 text-[#D29922]" />
              Audit Trail
            </button>
          </nav>
        </div>

        <div className="p-4 border-t border-[#30363D] bg-[#0D1117]/50">
          <div className="flex items-center justify-between text-xs text-[#8B949E]">
            <span className="flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-[#3FB950] animate-pulse"></span>
              Ktor Netty Online
            </span>
            <span className="font-mono">v0.1.0-RC</span>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 overflow-y-auto bg-[#0D1117] p-8">
        {/* Header bar */}
        <header className="flex items-center justify-between pb-6 mb-8 border-b border-[#30363D]">
          <div>
            <h2 className="text-xl font-bold tracking-tight text-white capitalize">
              {activeTab === 'overview' && 'System Health & Fleet Telemetry'}
              {activeTab === 'profiles' && 'VPN Profiles & Immutable Versions'}
              {activeTab === 'devices' && 'Device Trust & Enrollment Gateway'}
              {activeTab === 'audit' && 'Tamper-Evident Redacted Audit Logs'}
            </h2>
            <p className="text-xs text-[#8B949E] mt-1">
              Cloudflare Kumo UI Token Standard &bull; Production Security Control Plane
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => setIsEnrollModalOpen(true)}
              className="flex items-center gap-2 px-3.5 py-2 bg-[#F38020] hover:bg-[#E57213] text-white rounded-lg text-xs font-semibold shadow-sm transition"
            >
              <Key className="w-4 h-4" />
              Generate Token
            </button>
            <button
              onClick={() => setIsUploadModalOpen(true)}
              className="flex items-center gap-2 px-3.5 py-2 bg-[#21262D] hover:bg-[#30363D] border border-[#30363D] text-[#E6EDF3] rounded-lg text-xs font-medium transition"
            >
              <Plus className="w-4 h-4" />
              Upload .ovpn
            </button>
          </div>
        </header>

        {/* Tab 1: Overview */}
        {activeTab === 'overview' && (
          <div className="space-y-8">
            {/* Metric Cards Grid */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="p-5 rounded-xl bg-[#161B22] border border-[#30363D]">
                <div className="flex items-center justify-between text-[#8B949E] text-xs font-medium">
                  <span>Managed Profiles</span>
                  <FileText className="w-4 h-4 text-[#F38020]" />
                </div>
                <div className="text-2xl font-bold text-white mt-2">2</div>
                <div className="text-xs text-[#3FB950] mt-1 flex items-center gap-1">
                  <CheckCircle2 className="w-3.5 h-3.5" /> 100% policy-compliant
                </div>
              </div>

              <div className="p-5 rounded-xl bg-[#161B22] border border-[#30363D]">
                <div className="flex items-center justify-between text-[#8B949E] text-xs font-medium">
                  <span>Enrolled Fleet</span>
                  <Smartphone className="w-4 h-4 text-[#58A6FF]" />
                </div>
                <div className="text-2xl font-bold text-white mt-2">231</div>
                <div className="text-xs text-[#8B949E] mt-1">
                  142 Android &bull; 89 macOS
                </div>
              </div>

              <div className="p-5 rounded-xl bg-[#161B22] border border-[#30363D]">
                <div className="flex items-center justify-between text-[#8B949E] text-xs font-medium">
                  <span>Sync Health (24h)</span>
                  <Activity className="w-4 h-4 text-[#3FB950]" />
                </div>
                <div className="text-2xl font-bold text-white mt-2">99.98%</div>
                <div className="text-xs text-[#3FB950] mt-1">
                  0 Rollbacks reported
                </div>
              </div>

              <div className="p-5 rounded-xl bg-[#161B22] border border-[#30363D]">
                <div className="flex items-center justify-between text-[#8B949E] text-xs font-medium">
                  <span>Server Signer</span>
                  <Lock className="w-4 h-4 text-[#D29922]" />
                </div>
                <div className="text-sm font-mono font-bold text-white mt-2">Ed25519</div>
                <div className="text-xs text-[#8B949E] font-mono truncate mt-1">
                  SHA-256: 8a72...4e1f
                </div>
              </div>
            </div>

            {/* Profiles Summary List */}
            <div className="rounded-xl bg-[#161B22] border border-[#30363D] overflow-hidden">
              <div className="p-4 border-b border-[#30363D] flex items-center justify-between">
                <h3 className="text-sm font-semibold text-white">Active Profile Deployments</h3>
                <span className="text-xs text-[#8B949E]">Real-time publication state</span>
              </div>
              <div className="divide-y divide-[#30363D]">
                {profiles.map(p => (
                  <div key={p.id} className="p-4 flex items-center justify-between hover:bg-[#21262D]/50 transition">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-sm text-white">{p.name}</span>
                        <span className="px-2 py-0.5 rounded text-[11px] font-mono bg-[#3FB950]/10 text-[#3FB950] border border-[#3FB950]/30">
                          STABLE: v{p.stableVersion}
                        </span>
                        {p.betaVersion !== p.stableVersion && (
                          <span className="px-2 py-0.5 rounded text-[11px] font-mono bg-[#D29922]/10 text-[#D29922] border border-[#D29922]/30">
                            BETA: v{p.betaVersion}
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-[#8B949E] mt-1">
                        ID: <code className="font-mono">{p.id}</code> &bull; {p.activeDevices} devices assigned &bull; {p.totalVersions} total versions
                      </p>
                    </div>
                    <button
                      onClick={() => {
                        setSelectedProfileId(p.id);
                        setActiveTab('profiles');
                      }}
                      className="flex items-center gap-1 text-xs text-[#F38020] hover:text-[#E57213] font-medium"
                    >
                      Manage <ChevronRight className="w-4 h-4" />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: Profile Registry */}
        {activeTab === 'profiles' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Profile Selector & List */}
            <div className="rounded-xl bg-[#161B22] border border-[#30363D] p-4 space-y-3">
              <h3 className="text-xs font-semibold text-[#8B949E] uppercase tracking-wider">Profiles Catalog</h3>
              <div className="space-y-2">
                {profiles.map(p => (
                  <button
                    key={p.id}
                    onClick={() => setSelectedProfileId(p.id)}
                    className={`w-full text-left p-3 rounded-lg border transition ${
                      selectedProfile.id === p.id 
                        ? 'bg-[#21262D] border-[#F38020]' 
                        : 'border-[#30363D] hover:bg-[#21262D]/50'
                    }`}
                  >
                    <div className="font-semibold text-sm text-white">{p.name}</div>
                    <div className="text-xs text-[#8B949E] mt-1">Stable: v{p.stableVersion} &bull; Beta: v{p.betaVersion}</div>
                  </button>
                ))}
              </div>
            </div>

            {/* Profile Detail & Version Manager */}
            <div className="lg:col-span-2 space-y-6">
              <div className="rounded-xl bg-[#161B22] border border-[#30363D] p-6">
                <div className="flex items-center justify-between pb-4 border-b border-[#30363D]">
                  <div>
                    <h3 className="text-lg font-bold text-white">{selectedProfile.name}</h3>
                    <p className="text-xs font-mono text-[#8B949E]">{selectedProfile.id}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-1 bg-[#3FB950]/10 text-[#3FB950] border border-[#3FB950]/30 rounded-md text-xs font-semibold">
                      Published Stable: v{selectedProfile.stableVersion}
                    </span>
                  </div>
                </div>

                {/* AST & Directive Validation Banner */}
                <div className="my-4 p-3.5 bg-[#010409] border border-[#30363D] rounded-lg text-xs space-y-1 font-mono">
                  <div className="text-[#3FB950] flex items-center gap-1.5">
                    <CheckCircle2 className="w-4 h-4" /> Validated Directives: client, dev tun, proto udp, remote, cipher AES-256-GCM
                  </div>
                  <div className="text-[#8B949E] flex items-center gap-1.5">
                    <Lock className="w-4 h-4" /> Blocked Directives: plugin, script-security, up, down, management (None detected)
                  </div>
                </div>

                {/* Versions Table */}
                <h4 className="text-xs font-semibold text-[#8B949E] uppercase tracking-wider mb-3">Version History & Promotion</h4>
                <div className="divide-y divide-[#30363D] border border-[#30363D] rounded-lg overflow-hidden">
                  {selectedProfile.versions.map(v => (
                    <div key={v.version} className="p-3.5 bg-[#161B22] flex items-center justify-between hover:bg-[#21262D]/50">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-mono font-bold text-sm text-white">v{v.version}</span>
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            v.channel === 'STABLE' ? 'bg-[#3FB950]/20 text-[#3FB950]' :
                            v.channel === 'BETA' ? 'bg-[#D29922]/20 text-[#D29922]' : 'bg-[#8B949E]/20 text-[#8B949E]'
                          }`}>
                            {v.channel}
                          </span>
                        </div>
                        <div className="text-xs font-mono text-[#8B949E] mt-1">
                          Digest: {v.digest} &bull; {v.createdAt}
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        {v.channel !== 'STABLE' && (
                          <button
                            onClick={() => handlePromote(v.version, 'STABLE')}
                            className="px-2.5 py-1 text-xs font-semibold bg-[#3FB950] hover:bg-[#2EA043] text-white rounded transition"
                          >
                            Promote to STABLE
                          </button>
                        )}
                        {v.channel !== 'BETA' && (
                          <button
                            onClick={() => handlePromote(v.version, 'BETA')}
                            className="px-2.5 py-1 text-xs font-semibold bg-[#21262D] hover:bg-[#30363D] text-[#D29922] border border-[#30363D] rounded transition"
                          >
                            Set BETA
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab 3: Devices Fleet */}
        {activeTab === 'devices' && (
          <div className="rounded-xl bg-[#161B22] border border-[#30363D] overflow-hidden">
            <div className="p-4 border-b border-[#30363D] flex items-center justify-between">
              <h3 className="text-sm font-semibold text-white">Enrolled Client Hardware</h3>
              <div className="text-xs text-[#8B949E]">Device Trust Inventory</div>
            </div>
            <table className="w-full text-left text-xs">
              <thead className="bg-[#010409] text-[#8B949E] border-b border-[#30363D]">
                <tr>
                  <th className="p-3.5 font-medium">Device & Platform</th>
                  <th className="p-3.5 font-medium">Assigned Profile</th>
                  <th className="p-3.5 font-medium">Channel</th>
                  <th className="p-3.5 font-medium">Status</th>
                  <th className="p-3.5 font-medium">Last Seen</th>
                  <th className="p-3.5 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#30363D]">
                {devices.map(d => (
                  <tr key={d.id} className="hover:bg-[#21262D]/50 transition">
                    <td className="p-3.5">
                      <div className="font-semibold text-white flex items-center gap-1.5">
                        {d.platform === 'android' ? <Smartphone className="w-4 h-4 text-[#3FB950]" /> : <Laptop className="w-4 h-4 text-[#58A6FF]" />}
                        {d.name}
                      </div>
                      <div className="text-[11px] font-mono text-[#8B949E]">{d.publicKey}</div>
                    </td>
                    <td className="p-3.5 font-mono">{d.profileId}</td>
                    <td className="p-3.5">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-[#30363D] text-[#E6EDF3]">
                        {d.channel}
                      </span>
                    </td>
                    <td className="p-3.5">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        d.status === 'ACTIVE' ? 'bg-[#3FB950]/10 text-[#3FB950] border border-[#3FB950]/30' : 'bg-[#F85149]/10 text-[#F85149] border border-[#F85149]/30'
                      }`}>
                        {d.status}
                      </span>
                    </td>
                    <td className="p-3.5 text-[#8B949E]">{d.lastSeen}</td>
                    <td className="p-3.5 text-right">
                      {d.status === 'ACTIVE' ? (
                        <button
                          onClick={() => handleRevokeDevice(d.id)}
                          className="px-2.5 py-1 text-xs font-semibold bg-[#F85149]/10 hover:bg-[#F85149]/20 text-[#F85149] border border-[#F85149]/30 rounded transition"
                        >
                          Revoke
                        </button>
                      ) : (
                        <span className="text-xs text-[#8B949E]">Revoked</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Tab 4: Audit Trail */}
        {activeTab === 'audit' && (
          <div className="rounded-xl bg-[#161B22] border border-[#30363D] overflow-hidden space-y-4 p-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold text-white">Immutable Operator Audit Trail</h3>
              <div className="flex items-center gap-2 text-xs text-[#8B949E]">
                <Shield className="w-4 h-4 text-[#3FB950]" /> Verified Redaction Active
              </div>
            </div>

            <div className="space-y-2">
              {auditLogs.map(log => (
                <div key={log.id} className="p-3.5 rounded-lg bg-[#010409] border border-[#30363D] font-mono text-xs space-y-1">
                  <div className="flex items-center justify-between text-[#8B949E]">
                    <span className="text-white font-bold">{log.action}</span>
                    <span>{log.timestamp}</span>
                  </div>
                  <div className="text-xs text-[#58A6FF]">Actor: {log.actor} &bull; Target: {log.target}</div>
                  <div className="p-2 rounded bg-[#0D1117] text-[#8B949E] text-[11px] truncate">
                    Payload: {log.redactedPayload}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>

      {/* Enrollment Token Generator Modal */}
      {isEnrollModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-[#161B22] border border-[#30363D] rounded-xl max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between pb-3 border-b border-[#30363D]">
              <h3 className="font-bold text-white text-base">Generate Single-Use Token</h3>
              <button onClick={() => setIsEnrollModalOpen(false)} className="text-[#8B949E] hover:text-white">
                <XCircle className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-3 text-xs">
              <div>
                <label className="block text-[#8B949E] mb-1">Target Platform</label>
                <select className="w-full bg-[#010409] border border-[#30363D] rounded p-2 text-white">
                  <option>Android (Compose)</option>
                  <option>macOS (SwiftUI)</option>
                </select>
              </div>
              <div>
                <label className="block text-[#8B949E] mb-1">Assigned Profile</label>
                <select className="w-full bg-[#010409] border border-[#30363D] rounded p-2 text-white">
                  <option>edge-production-eu</option>
                  <option>corp-internal-us</option>
                </select>
              </div>
              <div>
                <label className="block text-[#8B949E] mb-1">Generated Single-Use Token (TTL: 15m)</label>
                <div className="p-2.5 bg-[#010409] border border-[#30363D] rounded font-mono text-[#F38020] flex items-center justify-between">
                  <span>tok_enroll_8f293b1029ab</span>
                  <button onClick={() => showToast('Token copied to clipboard')} className="text-[#8B949E] hover:text-white">
                    <Copy className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
            <div className="pt-2 flex justify-end gap-2">
              <button onClick={() => setIsEnrollModalOpen(false)} className="px-3.5 py-1.5 bg-[#21262D] text-white rounded text-xs">
                Done
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Upload .ovpn Modal */}
      {isUploadModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-[#161B22] border border-[#30363D] rounded-xl max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between pb-3 border-b border-[#30363D]">
              <h3 className="font-bold text-white text-base">Upload OpenVPN Configuration</h3>
              <button onClick={() => setIsUploadModalOpen(false)} className="text-[#8B949E] hover:text-white">
                <XCircle className="w-5 h-5" />
              </button>
            </div>
            <p className="text-xs text-[#8B949E]">
              Upload an immutable <code>.ovpn</code> profile artifact. AST checks will enforce directive safety rules.
            </p>
            <div className="p-6 border-2 border-dashed border-[#30363D] rounded-lg text-center space-y-2">
              <FileText className="w-8 h-8 text-[#F38020] mx-auto" />
              <div className="text-xs text-white">Drag and drop profile.ovpn here, or browse</div>
            </div>
            <div className="pt-2 flex justify-end gap-2">
              <button onClick={() => setIsUploadModalOpen(false)} className="px-3.5 py-1.5 bg-[#21262D] text-white rounded text-xs">
                Cancel
              </button>
              <button 
                onClick={() => {
                  setIsUploadModalOpen(false);
                  showToast('Uploaded v10 for edge-production-eu successfully validated!');
                }} 
                className="px-3.5 py-1.5 bg-[#F38020] text-white rounded text-xs font-semibold"
              >
                Validate & Ingest
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
