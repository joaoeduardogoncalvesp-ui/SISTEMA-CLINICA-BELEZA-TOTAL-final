package com.clinicabelezatotal.service;

import com.clinicabelezatotal.dto.LoginDTO;
import com.clinicabelezatotal.dto.LoginResponseDTO;
import com.clinicabelezatotal.dto.UsuarioDTO;
import com.clinicabelezatotal.exception.ResourceNotFoundException;
import com.clinicabelezatotal.model.Funcionario;
import com.clinicabelezatotal.model.Usuario;
import com.clinicabelezatotal.model.Usuario.PerfilUsuario;
import com.clinicabelezatotal.repository.FuncionarioRepository;
import com.clinicabelezatotal.repository.UsuarioRepository;
import com.clinicabelezatotal.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponseDTO login(LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getSenha())
        );
        
        Usuario usuario = usuarioRepository.findByEmail(loginDTO.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        
        String token = jwtTokenProvider.generateToken(authentication);
        
        return new LoginResponseDTO(
            token,
            "Bearer",
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getPerfil().name()
        );
    }

    public UsuarioDTO cadastrar(UsuarioDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email já cadastrado");
        }
        if (dto.getSenha() == null || dto.getSenha().isEmpty()) {
            throw new RuntimeException("Senha é obrigatória");
        }
        if (dto.getSenha().length() < 8) {
            throw new RuntimeException("Senha deve ter no mínimo 8 caracteres");
        }
        
        Funcionario funcionario = resolverFuncionario(dto.getFuncionarioId(), null);
        if (dto.getPerfil() == PerfilUsuario.FUNCIONARIO && funcionario == null) {
            throw new RuntimeException("Perfil FUNCIONARIO exige vínculo com um funcionário");
        }
        
        Usuario usuario = Usuario.builder()
            .nome(dto.getNome())
            .email(dto.getEmail())
            .senha(passwordEncoder.encode(dto.getSenha()))
            .perfil(dto.getPerfil())
            .funcionario(funcionario)
            .ativo(true)
            .build();
        
        usuario = usuarioRepository.save(usuario);
        dto.setId(usuario.getId());
        dto.setSenha(null);
        return dto;
    }

    public List<UsuarioDTO> listarTodos() {
        return usuarioRepository.findAllAtivos().stream()
            .map(this::toDTO)
            .toList();
    }

    public UsuarioDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return toDTO(usuario);
    }

    public UsuarioDTO atualizar(Long id, UsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        
        Funcionario funcionario = resolverFuncionario(dto.getFuncionarioId(), id);
        if (dto.getPerfil() == PerfilUsuario.FUNCIONARIO && funcionario == null) {
            throw new RuntimeException("Perfil FUNCIONARIO exige vínculo com um funcionário");
        }
        
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        if (dto.getSenha() != null && !dto.getSenha().isEmpty()) {
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        usuario.setPerfil(dto.getPerfil());
        usuario.setFuncionario(funcionario);
        
        usuario = usuarioRepository.save(usuario);
        return toDTO(usuario);
    }

    private Funcionario resolverFuncionario(Long funcionarioId, Long usuarioId) {
        if (funcionarioId == null) {
            return null;
        }
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));
        Optional<Usuario> vinculado = usuarioRepository.findByFuncionarioId(funcionarioId);
        if (vinculado.isPresent() && !vinculado.get().getId().equals(usuarioId)) {
            throw new RuntimeException("Funcionário já está vinculado a outro usuário");
        }
        return funcionario;
    }

    public void excluir(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        usuarioRepository.delete(usuario);
    }

    private UsuarioDTO toDTO(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setNome(usuario.getNome());
        dto.setEmail(usuario.getEmail());
        dto.setPerfil(usuario.getPerfil());
        dto.setFuncionarioId(usuario.getFuncionario() != null ? usuario.getFuncionario().getId() : null);
        return dto;
    }
}
